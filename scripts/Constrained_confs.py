from rdkit import Chem
from rdkit.Chem import rdFMCS, AllChem, rdMolAlign
import sys


def get_conformers(smiles=None, anchor=None, num_confs=None, output=None,rmsd_threshold=1):
    mol = Chem.MolFromSmiles(smiles,False)
    AllChem.EmbedMolecule(mol)

    constrain = Chem.SDMolSupplier(anchor,False)[0]

    r = rdFMCS.FindMCS([mol, constrain])
    a = mol.GetSubstructMatch(Chem.MolFromSmarts(r.smartsString))
    b = constrain.GetSubstructMatch(Chem.MolFromSmarts(r.smartsString))
    amap = list(zip(a, b))
    coors = dict()

    for a,b in amap:
        coors[a] = constrain.GetConformer().GetAtomPosition(b)

    w = Chem.SDWriter(output)

    mol.UpdatePropertyCache()
    constrain.UpdatePropertyCache()
    
    #AllChem.EmbedMolecule(mol,coordMap=coors,forceTol=0.01)
    #mp = AllChem.MMFFGetMoleculeProperties(mol, mmffVariant='MMFF94s')
    #ff = AllChem.MMFFGetMoleculeForceField(mol, mp)
    #for i in mol.GetSubstructMatch(constrain):
        #ff.MMFFAddPositionConstraint(i, 0, 1.0e5)

    confs = AllChem.EmbedMultipleConfs(mol,
                                       numConfs=int(num_confs),
                                       pruneRmsThresh=0.75,
                                       coordMap=coors,
                                       enforceChirality=True,
                                       useExpTorsionAnglePrefs=True,
                                       useBasicKnowledge=True
                                       )
    for element in confs:
        Chem.SanitizeMol(mol)
        rmsd = AllChem.GetBestRMS(mol,constrain,element,0,map=[list(amap)])
        #rmsd = rdMolAlign.AlignMol(mol, constrain, element, 0, atomMap=amap)
        if rmsd<=float(rmsd_threshold):
            w.write(mol, confId=element)
    w.close()


def main():
    print('''
        USAGE: python Constrained_confs.py smiles_code anchor.sdf num_confs output.sdf
        ''')

    get_conformers(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4],sys.argv[5])


if __name__ == '__main__':
    main()
