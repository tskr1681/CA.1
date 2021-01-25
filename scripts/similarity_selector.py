import random
import sys

import numpy as np
import pandas as pd
from rdkit import Chem, DataStructs
from rdkit.Chem import AllChem, MACCSkeys
from rdkit.Chem.Fingerprints import FingerprintMols
from rdkit.DataManip.Metric.rdMetricMatrixCalc import GetTanimotoSimMat
from sklearn import datasets, decomposition
from sklearn.cluster import DBSCAN
from sklearn.manifold import TSNE
from sklearn.metrics import davies_bouldin_score


def get_distance_matrix(smiles):
    smi = (Chem.MolFromSmiles(x) for x in smiles)
    fps = [MACCSkeys.GenMACCSKeys(x) for x in smi]
    tanimoto_sim_mat_lower_triangle = GetTanimotoSimMat(fps)
    n_mol = len(fps)
    similarity_matrix = np.ones([n_mol, n_mol])
    i_lower = np.tril_indices(n=n_mol, k=-1)
    i_upper = np.triu_indices(n=n_mol, k=1)
    similarity_matrix[i_lower] = tanimoto_sim_mat_lower_triangle
    similarity_matrix[i_upper] = similarity_matrix.T[i_upper]
    return np.subtract(1, similarity_matrix)


def get_optimal_dbscan_dist(TSNE_sim):
    range_dist = range(1, 60)
    result = []
    for dist in range_dist:
        try:
            davies_bouldin = davies_bouldin_score(TSNE_sim, DBSCAN(eps=(dist / 10)).fit(TSNE_sim).labels_)
            result.append((dist / 10, davies_bouldin))
        except ValueError:
            pass
    return min(result, key=lambda x: x[1])[0]


def get_compounds(table, seed):
    cluster_labels = set(table['COLOR'])
    if -1 in cluster_labels:
        cluster_labels.remove(-1)
    random.seed(seed)
    compounds = []
    for c in cluster_labels:
        cluster_smiles = list(table[table['COLOR'] == c]['SMILES'])
        compounds.extend(random.sample(cluster_smiles, int(len(cluster_smiles) * 0.3)))
    return compounds


def get_optimal_perlexity(data_size):
    optimal = 50 if data_size >= 1000 else (30 if data_size >= 200 else 5)
    return optimal


def main(input_file, output_file, seed):
    table = pd.DataFrame()
    i = 0
    with open(input_file) as file:
        for line in file:
            if Chem.MolFromSmiles(line.strip()) is not None:
                table.loc[i, 'SMILES'] = line.strip()
                i += 1

    smiles = table['SMILES']
    distance_matrix = get_distance_matrix(smiles)
    if np.mean(distance_matrix) > 0.75 or len(smiles) < 100:
        print("Compound similarity is too high or the library is too small for clustering, aborting and writing input file to output",
              file=sys.stderr)
        with open(output_file, "w") as file:
            for compound in smiles:
                file.write(compound + "\n")
        return

    optimal_perplexity = get_optimal_perlexity(len(smiles))
    TSNE_sim = TSNE(n_components=2, init='pca', random_state=seed, angle=0.5, perplexity=optimal_perplexity).fit_transform(
        distance_matrix)

    optimal_eps = get_optimal_dbscan_dist(TSNE_sim)

    dbscan_result = DBSCAN(eps=optimal_eps).fit(TSNE_sim)

    tsne_result = pd.DataFrame(data=TSNE_sim, columns=["TC1", "TC2"])
    table['TC1'] = tsne_result['TC1']
    table['TC2'] = tsne_result['TC2']
    table['COLOR'] = dbscan_result.labels_

    compounds = get_compounds(table, seed)

    with open(output_file, "w") as file:
        for compound in compounds:
            file.write(compound + "\n")


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2], int(sys.argv[3]))
