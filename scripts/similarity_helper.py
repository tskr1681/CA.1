import sys

from rdkit import Chem, DataStructs
from rdkit.Chem.Fingerprints import FingerprintMols


def main():
    reference = int(sys.argv[2])
    with open(sys.argv[1]) as smiles:
        smiles_list = list(smiles)
        mols = (get_mol(mol) for mol in smiles_list)
    reference = FingerprintMols.FingerprintMol(smiles_list[reference])
    fingerprints = (FingerprintMols.FingerprintMol(mol) if mol is not None else None for mol in mols)
    similarities = (DataStructs.FingerprintSimilarity(reference, fingerprint) if fingerprint is not None else 0 for fingerprint in fingerprints)
    for similarity in similarities:
        print(similarity)


def get_mol(smiles):
    Chem.MolFromSmiles(smiles)

if __name__ == "__main__":
    main()
