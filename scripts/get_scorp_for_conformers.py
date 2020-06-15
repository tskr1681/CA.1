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

pymol_widget_b64 = "ZnJvbSBweW1vbC53aXphcmQgaW1wb3J0IFdpemFyZApmcm9tIHB5bW9sIGltcG9ydCBjbWQsIHN0b3JlZAppbXBvcnQgcHltb2wKCmludGVyYWN0aW9uX3R5cGVzID0gWwogICAgICAgICJoYm9uZCIsCiAgICAgICAgImlvbmljIiwKICAgICAgICAiY2F0LWRpcCIsCiAgICAgICAgImNhdC1waSIsCiAgICAgICAgImRpcG9sYXIiLAogICAgICAgICJoYWxvZ2VuIiwKICAgICAgICAiZG9uLXBpIiwKICAgICAgICAicGktcGkiLAogICAgICAgICJ2ZHciLAogICAgICAgICJ1bmZhdm9yYWJsZV9pb25pYyIsCiAgICAgICAgInVuZmF2b3JhYmxlX2hib25kIiwKICAgICAgICAidW5mYXZvcmFibGVfZGlwb2xlIiwKICAgICAgICAidW5mYXZvcmFibGVfdmR3IgogICAgICAgICJwb29yX2FuZyIsCiAgICAgICAgInVuY2xhc3MiCiAgICBdCgoKY2xhc3MgU2NvcnBXaXphcmQoV2l6YXJkKToKICAgIGRlZiBfX2luaXRfXyhzZWxmLCBmaWxlLCBzY29yZXMpOgogICAgICAgIHNlbGYuX3Byb21wdCA9IFsnVGhpcyBpcyB3b3JrIGluIHByb2dyZXNzJ10KICAgICAgICBzZWxmLl9maWxlID0gZmlsZQogICAgICAgIHNlbGYuX2NvbmZfYW1vdW50ID0gc2VsZi5nZXRfY29uZl9hbW91bnQoKQogICAgICAgIHNlbGYuX3Njb3JlcyA9IHNjb3JlcwogICAgICAgIHNlbGYuX3Njb3JlX3RvZ2dsZSA9IFRydWUKICAgICAgICB0ZW1wID0gWyhpICsgMSwgc2NvcmUpIGZvciBpLCBzY29yZSBpbiBlbnVtZXJhdGUoc2NvcmVzKV0KICAgICAgICBzb3J0ZWRfc2NvcmVzID0gc29ydGVkKHRlbXAsIGtleT1sYW1iZGEgdHVwOiAtdHVwWzFdKQogICAgICAgIGZvciBzY29yZSBpbiBzb3J0ZWRfc2NvcmVzOgogICAgICAgICAgICBwcmludChmIkNvbmZvcm1lciB7c2NvcmVbMF19IGhhcyBhIHNjb3JlIG9mIHtzY29yZVsxXX0iKQoKICAgIGRlZiBuZXh0X2NvbmYoc2VsZik6CiAgICAgICAgY3VyX2NvbmYgPSBjbWQuZ2V0X3N0YXRlKCkKICAgICAgICBuZXdfY29uZiA9IGN1cl9jb25mICUgc2VsZi5fY29uZl9hbW91bnQgKyAxCiAgICAgICAgY21kLnNldCgic3RhdGUiLCBuZXdfY29uZikKICAgICAgICBmb3IgaXR5cGUgaW4gaW50ZXJhY3Rpb25fdHlwZXM6CiAgICAgICAgICAgIGNtZC5kaXNhYmxlKGYie2l0eXBlfSoiKQogICAgICAgICAgICBjbWQuZW5hYmxlKGYie2l0eXBlfXtuZXdfY29uZn0iLCAxKQogICAgICAgIGlmIHNlbGYuX3Njb3JlX3RvZ2dsZToKICAgICAgICAgICAgY21kLmRpc2FibGUoInNjb3JwX3Njb3JlcyoiKQogICAgICAgICAgICBjbWQuZW5hYmxlKGYic2NvcnBfc2NvcmVze25ld19jb25mfSIsIDEpCiAgICAgICAgcHJpbnQoIlNjb3JwaW9uIHNjb3JlIGZvciB0aGlzIGNvbmZvcm1lcjogIiArIHN0cihzZWxmLl9zY29yZXNbbmV3X2NvbmYtMV0pKQoKICAgIGRlZiBwcmV2X2NvbmYoc2VsZik6CiAgICAgICAgY3VyX2NvbmYgPSBjbWQuZ2V0X3N0YXRlKCkKICAgICAgICBuZXdfY29uZiA9IGN1cl9jb25mIC0gMSBpZiBjdXJfY29uZiA+IDEgZWxzZSBzZWxmLl9jb25mX2Ftb3VudAogICAgICAgIGNtZC5zZXQoInN0YXRlIiwgbmV3X2NvbmYpCiAgICAgICAgZm9yIGl0eXBlIGluIGludGVyYWN0aW9uX3R5cGVzOgogICAgICAgICAgICBjbWQuZGlzYWJsZShmIntpdHlwZX0qIikKICAgICAgICAgICAgY21kLmVuYWJsZShmIntpdHlwZX17bmV3X2NvbmZ9IiwgMSkKICAgICAgICBpZiBzZWxmLl9zY29yZV90b2dnbGU6CiAgICAgICAgICAgIGNtZC5kaXNhYmxlKCJzY29ycF9zY29yZXMqIikKICAgICAgICAgICAgY21kLmVuYWJsZShmInNjb3JwX3Njb3Jlc3tuZXdfY29uZn0iLCAxKQogICAgICAgIHByaW50KCJTY29ycGlvbiBzY29yZSBmb3IgdGhpcyBjb25mb3JtZXI6ICIgKyBzdHIoc2VsZi5fc2NvcmVzW25ld19jb25mLTFdKSkKCiAgICBkZWYgZ2V0X2NvbmZfYW1vdW50KHNlbGYpOgogICAgICAgIGxpZ19uYW1lID0gc2VsZi5fZmlsZQogICAgICAgIHNlbGYuX3Byb21wdCA9IFsiVGVzdCJdCiAgICAgICAgc3RvcmVkLmNvbmZzID0gMAogICAgICAgIGNtZC5pdGVyYXRlX3N0YXRlKDAsIHNlbGYuX2ZpbGUgKyAiIGFuZCByYW5rIDEiLCAic3RvcmVkLmNvbmZzICs9IDEiKQogICAgICAgIHJldHVybiBzdG9yZWQuY29uZnMKCiAgICBkZWYgdG9nZ2xlX3Njb3JlcyhzZWxmKToKICAgICAgICBzZWxmLl9zY29yZV90b2dnbGUgPSBub3Qgc2VsZi5fc2NvcmVfdG9nZ2xlCiAgICAgICAgaWYgc2VsZi5fc2NvcmVfdG9nZ2xlOgogICAgICAgICAgICBjbWQuZW5hYmxlKGYic2NvcnBfc2NvcmVze2NtZC5nZXRfc3RhdGUoKX0iLCAxKQogICAgICAgIGVsc2U6CiAgICAgICAgICAgIGNtZC5kaXNhYmxlKCJzY29ycF9zY29yZXMqIikKCiAgICBkZWYgZ2V0X3BhbmVsKHNlbGYpOgogICAgICAgIHJldHVybiBbCiAgICAgICAgICAgIFsxLCAnQ29uZm9ybWVyIFNlbGVjdGlvbicsICcnXSwKICAgICAgICAgICAgWzIsICdOZXh0JywgJ2NtZC5nZXRfd2l6YXJkKCkubmV4dF9jb25mKCknXSwKICAgICAgICAgICAgWzIsICdQcmV2aW91cycsICdjbWQuZ2V0X3dpemFyZCgpLnByZXZfY29uZigpJ10sCiAgICAgICAgICAgIFsyLCAnVG9nZ2xlIFNjb3JlcycsICdjbWQuZ2V0X3dpemFyZCgpLnRvZ2dsZV9zY29yZXMoKSddLAogICAgICAgICAgICBbMiwgJ0V4aXQnLCAnY21kLnNldF93aXphcmQoKSddLAogICAgICAgICAgICBdCgogICAgZGVmIGdldF9wcm9tcHQoc2VsZik6CiAgICAgICAgc2VsZi5wcm9tcHQgPSBzZWxmLl9wcm9tcHQKICAgICAgICByZXR1cm4gc2VsZi5wcm9tcHQ="

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
                "color grey, unfavorable_hbond",
                "color grey, unfavorable_dipole",
                "color grey, unfavorable_vdw",
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
            if "unfavorable_vdw" in interaction_type:
                interaction_type = "unfavorable_vdw"
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
        "unfavorable_vdw",
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
