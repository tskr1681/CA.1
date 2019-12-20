"""
Usage: python get_scorp_for_conformers.py sdf_file receptor_file pymol_script

Collects data from a scorpion processed sdf file, and uses it to generate scorpion-like representations for all conformers.
Outputs to a pymol script

Arguments:
  sdf_file: The sdf file containg the scorpion data
  receptor_file: The receptor file that was used to make the sdf file
  pymol_script: The name of the pymol script that you want to generate
"""

import sys
import re
import base64
import os.path

pymol_widget_b64 = "ZnJvbSBweW1vbC53aXphcmQgaW1wb3J0IFdpemFyZApmcm9tIHB5bW9sIGltcG9ydCBjbWQsIHN0b3JlZAppbXBvcnQgcHltb2wKCmludGVyYWN0aW9uX3R5cGVzID0gWwogICAgICAgICJoYm9uZCIsCiAgICAgICAgImlvbmljIiwKICAgICAgICAiY2F0LWRpcCIsCiAgICAgICAgImNhdC1waSIsCiAgICAgICAgImRpcG9sYXIiLAogICAgICAgICJoYWxvZ2VuIiwKICAgICAgICAiZG9uLXBpIiwKICAgICAgICAicGktcGkiLAogICAgICAgICJ2ZHciLAogICAgICAgICJ1bmZhdm9yYWJsZV9pb25pYyIsCiAgICAgICAgInVuZmF2b3JhYmxlX2hib25kIiwKICAgICAgICAidW5mYXZvcmFibGVfZGlwb2xlIiwKICAgICAgICAicG9vcl9hbmciLAogICAgICAgICJ1bmNsYXNzIgogICAgXQoKCmNsYXNzIFNjb3JwV2l6YXJkKFdpemFyZCk6CiAgICBkZWYgX19pbml0X18oc2VsZiwgZmlsZSwgc2NvcmVzKToKICAgICAgICBzZWxmLl9wcm9tcHQgPSBbJ1RoaXMgaXMgd29yayBpbiBwcm9ncmVzcyddCiAgICAgICAgc2VsZi5fZmlsZSA9IGZpbGUKICAgICAgICBzZWxmLl9jb25mX2Ftb3VudCA9IHNlbGYuZ2V0X2NvbmZfYW1vdW50KCkKICAgICAgICBzZWxmLl9zY29yZXMgPSBzY29yZXMKICAgICAgICBzZWxmLl9zY29yZV90b2dnbGUgPSBUcnVlCiAgICAgICAgdGVtcCA9IFsoaSArIDEsIHNjb3JlKSBmb3IgaSwgc2NvcmUgaW4gZW51bWVyYXRlKHNjb3JlcyldCiAgICAgICAgc29ydGVkX3Njb3JlcyA9IHNvcnRlZCh0ZW1wLCBrZXk9bGFtYmRhIHR1cDogLXR1cFsxXSkKICAgICAgICBmb3Igc2NvcmUgaW4gc29ydGVkX3Njb3JlczoKICAgICAgICAgICAgcHJpbnQoZiJDb25mb3JtZXIge3Njb3JlWzBdfSBoYXMgYSBzY29yZSBvZiB7c2NvcmVbMV19IikKCiAgICBkZWYgbmV4dF9jb25mKHNlbGYpOgogICAgICAgIGN1cl9jb25mID0gY21kLmdldF9zdGF0ZSgpCiAgICAgICAgbmV3X2NvbmYgPSBjdXJfY29uZiAlIHNlbGYuX2NvbmZfYW1vdW50ICsgMQogICAgICAgIGNtZC5zZXQoInN0YXRlIiwgbmV3X2NvbmYpCiAgICAgICAgZm9yIGl0eXBlIGluIGludGVyYWN0aW9uX3R5cGVzOgogICAgICAgICAgICBjbWQuZGlzYWJsZShmIntpdHlwZX0qIikKICAgICAgICAgICAgY21kLmVuYWJsZShmIntpdHlwZX17bmV3X2NvbmZ9IiwgMSkKICAgICAgICBpZiBzZWxmLl9zY29yZV90b2dnbGU6CiAgICAgICAgICAgIGNtZC5kaXNhYmxlKCJzY29ycF9zY29yZXMqIikKICAgICAgICAgICAgY21kLmVuYWJsZShmInNjb3JwX3Njb3Jlc3tuZXdfY29uZn0iLCAxKQogICAgICAgIHByaW50KCJTY29ycGlvbiBzY29yZSBmb3IgdGhpcyBjb25mb3JtZXI6ICIgKyBzdHIoc2VsZi5fc2NvcmVzW25ld19jb25mLTFdKSkKCiAgICBkZWYgcHJldl9jb25mKHNlbGYpOgogICAgICAgIGN1cl9jb25mID0gY21kLmdldF9zdGF0ZSgpCiAgICAgICAgbmV3X2NvbmYgPSBjdXJfY29uZiAtIDEgaWYgY3VyX2NvbmYgPiAxIGVsc2Ugc2VsZi5fY29uZl9hbW91bnQKICAgICAgICBjbWQuc2V0KCJzdGF0ZSIsIG5ld19jb25mKQogICAgICAgIGZvciBpdHlwZSBpbiBpbnRlcmFjdGlvbl90eXBlczoKICAgICAgICAgICAgY21kLmRpc2FibGUoZiJ7aXR5cGV9KiIpCiAgICAgICAgICAgIGNtZC5lbmFibGUoZiJ7aXR5cGV9e25ld19jb25mfSIsIDEpCiAgICAgICAgaWYgc2VsZi5fc2NvcmVfdG9nZ2xlOgogICAgICAgICAgICBjbWQuZGlzYWJsZSgic2NvcnBfc2NvcmVzKiIpCiAgICAgICAgICAgIGNtZC5lbmFibGUoZiJzY29ycF9zY29yZXN7bmV3X2NvbmZ9IiwgMSkKICAgICAgICBwcmludCgiU2NvcnBpb24gc2NvcmUgZm9yIHRoaXMgY29uZm9ybWVyOiAiICsgc3RyKHNlbGYuX3Njb3Jlc1tuZXdfY29uZi0xXSkpCgogICAgZGVmIGdldF9jb25mX2Ftb3VudChzZWxmKToKICAgICAgICBsaWdfbmFtZSA9IHNlbGYuX2ZpbGUKICAgICAgICBzZWxmLl9wcm9tcHQgPSBbIlRlc3QiXQogICAgICAgIHN0b3JlZC5jb25mcyA9IDAKICAgICAgICBjbWQuaXRlcmF0ZV9zdGF0ZSgwLCBzZWxmLl9maWxlICsgIiBhbmQgcmFuayAxIiwgInN0b3JlZC5jb25mcyArPSAxIikKICAgICAgICByZXR1cm4gc3RvcmVkLmNvbmZzCgogICAgZGVmIHRvZ2dsZV9zY29yZXMoc2VsZik6CiAgICAgICAgc2VsZi5fc2NvcmVfdG9nZ2xlID0gbm90IHNlbGYuX3Njb3JlX3RvZ2dsZQogICAgICAgIGlmIHNlbGYuX3Njb3JlX3RvZ2dsZToKICAgICAgICAgICAgY21kLmVuYWJsZShmInNjb3JwX3Njb3Jlc3tjbWQuZ2V0X3N0YXRlKCl9IiwgMSkKICAgICAgICBlbHNlOgogICAgICAgICAgICBjbWQuZGlzYWJsZSgic2NvcnBfc2NvcmVzKiIpCgogICAgZGVmIGdldF9wYW5lbChzZWxmKToKICAgICAgICByZXR1cm4gWwogICAgICAgICAgICBbMSwgJ0NvbmZvcm1lciBTZWxlY3Rpb24nLCAnJ10sCiAgICAgICAgICAgIFsyLCAnTmV4dCcsICdjbWQuZ2V0X3dpemFyZCgpLm5leHRfY29uZigpJ10sCiAgICAgICAgICAgIFsyLCAnUHJldmlvdXMnLCAnY21kLmdldF93aXphcmQoKS5wcmV2X2NvbmYoKSddLAogICAgICAgICAgICBbMiwgJ1RvZ2dsZSBTY29yZXMnLCAnY21kLmdldF93aXphcmQoKS50b2dnbGVfc2NvcmVzKCknXSwKICAgICAgICAgICAgWzIsICdFeGl0JywgJ2NtZC5zZXRfd2l6YXJkKCknXSwKICAgICAgICAgICAgXQoKICAgIGRlZiBnZXRfcHJvbXB0KHNlbGYpOgogICAgICAgIHNlbGYucHJvbXB0ID0gc2VsZi5fcHJvbXB0CiAgICAgICAgcmV0dXJuIHNlbGYucHJvbXB0"

def get_contacts(file):
    """
    Gets the ligand-protein contact from an sdf file.
    :param file: The filename without the .sdf extension
    :return: a list of ligand-protein contacts as scorpion puts them in the sdf file, split in parts
    """
    magic_re = re.compile("(?<=')[^,]+?(?=')")  # looks for anything between quotes that does not contain a comma
    lines = [[]]
    scores = []
    with open(file) as conformer_file:
        in_contacts = False
        i = 0
        for line in conformer_file:
            if "TOTAL" in line and in_contacts:
                in_contacts = False
                i += 1
                lines.append([])
                scores.append(float(conformer_file.readline()))
            if in_contacts:
                re_results = magic_re.findall(line)
                if not len(re_results) == 0:
                    lines[i].append(re_results)
            if "CONTACTS" in line:
                in_contacts = True
    return lines, scores


def set_colors(i):
    """
    Sets colors for a set of interactions
    :param i: The set of interactions to color
    :return: a list of color commands, one for each interaction type
    """
    col_list = ["color red, hbond",
                "color pink, ionic",
                "color magenta, cat-dip",
                "color blue, cat-pi",
                "color cyan, dipolar",
                "color violet, halogen",
                "color green, don-pi",
                "color orange, pi-pi",
                "color yellow, vdw",
                "color grey, unfavorable_ionic",
                "color white, poor_ang",
                "color white, unclass"]
    out = []
    for col in col_list:
        out.append(col + str(i))
    return out


def get_scores(lines):
    """
    Parses contact lines to extract scorpion scores
    :param lines: a list of contacts
    :return: a list of dictionaries of scores per ligand atom
    """
    scores = []
    for i, conformer in enumerate(lines):
        scores.append({})
        for contact in conformer:
            scores[i][contact[0]] = float(contact[-1])
    return scores


def get_base_commands(sdf, rec, sdf_name):
    """
    Gets a list of commands the pymol script should start with
    :param sdf: the name of the sdf file to load
    :param rec: the name of the receptor file to load
    :param rec_name: the base name of the sdf file that was loaded, so without the extension
    :return: a list of pymol commands
    """
    return [f"load {rec}",
            "remove resn HET",
            f"load {sdf}",
            f"cur_index = []; iterate_state 0, ({sdf_name} and rank 1), cur_index.append(0)",
            f"alter_state 0, ({sdf_name}), cur_index[state-1] += 1; name = name + str(cur_index[state-1])"]


def get_dist_commands(lines, sdf, rec):
    """
    Gets a list of commands that produce all contacts as pymol distance commands
    :param lines: a list of contacts
    :param sdf: the sdf file that was loaded
    :return: a list of distance commands
    """
    i = 0
    commands = []
    for conformer in lines:
        for contact in conformer:
            ligand_atom = contact[0]
            interaction_type = contact[-3]
            residue_full = contact[-4].split()
            residue_name = residue_full[0]
            residue_chain = residue_full[1]
            residue_number = residue_full[2]
            commands.append(
                f"distance {interaction_type}{i+1}, /{sdf}///UNK`0/{ligand_atom}, /{rec}//{residue_chain}/{residue_name}`{residue_number}/{contact[1]}")
        commands.extend(set_colors(i+1))
        i += 1
    interaction_types = [
        "hbond",
        "ionic",
        "cat-dip",
        "cat-pi",
        "dipolar",
        "halogen",
        "don-pi",
        "pi-pi",
        "vdw",
        "unfavorable_ionic",
        "unfavorable_hbond",
        "unfavorable_dipole",
        "poor_ang",
        "unclass"
    ]
    for itype in interaction_types:
        commands.append(f"group {itype}, {itype}*")
        commands.append(f"disable {itype}*")
        commands.append(f"enable {itype}1, 1")
    commands.append(f"show sticks, {rec}")
    return commands


def get_score_commands(scores, sdf):
    """
    Produces a list of commands to show scorpion scores on ligand atoms
    :param scores: the scores to put on the atoms
    :param sdf: the sdf file that was loaded
    :return: a list of commands
    """
    commands = []
    for i, conformer in enumerate(scores):
        commands.append(f"create scorp_scores{i+1}, {sdf} and not hydro and not resn HOH and not ele ca+cu+fe+k+li+mg+mn+na+ni+pt+rb+ru+zn")
        commands.append(f"set sphere_scale, 0.40, scorp_scores{i+1}")
        commands.append(f"set sphere_transparency, 0.20, scorp_scores{i+1}")
        commands.append(f"show spheres, scorp_scores{i+1}")
        commands.append(f"hide sticks, scorp_scores{i+1}")
        commands.append(f"color gray, scorp_scores{i+1}")
        for atom, score in conformer.items():
            color = "br" + str(int(min(score / 1.5, 1)*6)+2) if score >= 0 else "grey10"
            commands.append(f"color {color}, /scorp_scores{i+1}///UNK`0/{atom}")
            commands.append(f"label /scorp_scores{i+1}///UNK`0/{atom}, '         %4.1f' % {score}")
    commands.append("group scorp_scores, scorp_scores*")
    commands.append("disable scorp_scores*")
    commands.append("enable scorp_scores1, 1")
    commands.append("load Pymol_widget.py")
    return commands


def main(file, rec, pymol_script):
    """
    Actually runs the program
    :param file: The file to read in and show output for
    :return: None
    """
    sdf_name = os.path.splitext(os.path.basename(file))[0]
    rec_name = os.path.splitext(os.path.basename(rec))[0]
    contacts, totals = get_contacts(file)
    scores = get_scores(contacts)
    base_commands = get_base_commands(file, rec, sdf_name)
    dist_commands = get_dist_commands(contacts, sdf_name, rec_name)
    score_commands = get_score_commands(scores, sdf_name)
    print(totals)
    with open(pymol_script, "w") as script:
        for line in base_commands:
            script.write(line + "\n")
        for line in dist_commands:
            script.write(line + "\n")
        for line in score_commands:
            script.write(line + "\n")
    with open("Pymol_widget.py", "w") as o:
        content = base64.b64decode(pymol_widget_b64)
        o.write(content.decode("utf-8"))
        o.write(f"\nscores={totals}")
        o.write(f'\n\ncmd.set_wizard(ScorpWizard("{sdf_name}",scores))')


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2], sys.argv[3])
