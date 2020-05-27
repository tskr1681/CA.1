import sys

from rdkit import Chem, DataStructs
from rdkit.Chem.Fingerprints import FingerprintMols


def main():
    reference = int(sys.argv[2])
    with open(sys.argv[1]) as smiles:
        mols = [Chem.MolFromSmiles(mol) for mol in smiles]
    fingerprints = [FingerprintMols.FingerprintMol(mol) if mol is not None else None for mol in mols]
    similarities = [DataStructs.FingerprintSimilarity(fingerprints[reference], fingerprint) if fingerprint is not None else 0 for fingerprint in fingerprints]
    for similarity in similarities:
        print(similarity)


if __name__ == "__main__":
    main()
