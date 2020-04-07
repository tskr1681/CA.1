const app = angular.module('compoundEvolver', ['fileReadBinding', 'angularjs-dropdown-multiselect']);

app.run(function ($rootScope) {
    $rootScope.generations = [];
    $rootScope.selectedGenerationNumber = null;
});

app.controller('FormInputCtrl', function ($scope, $rootScope) {

    // Define form model with default values
    $scope.formModel = {
        maxReactantWeight: 0,
        interspeciesCrossoverMethod: 'Complete',
        speciesDeterminationMethod: 'Dynamic',
        generationSize: 50,
        numberOfGenerations: 20,
        selectionSize: 0.5,
        mutationRate: 0.1,
        crossoverRate: 0.8,
        elitismRate: 0.1,
        randomImmigrantRate: 0.1,
        selectionMethod: 'Tournament selection',
        mutationMethod: 'Distance dependent',
        terminationCondition: 'fixed',
        nonImprovingGenerationQuantity: 0.3,
        conformerCount: 50,
        fitnessMeasure: 'ligandEfficiency',
        conformerOption: "ChemAxon",
        forceField: 'mab',
        scoringOption: 'scorpion',
        maxAnchorMinimizedRmsd: 1,
        exclusionShapeTolerance: 0,
        useLipinski: false,
        allowDuplicates: true,
        maxMolecularMass: 500,
        maxHydrogenBondDonors: 5,
        maxHydrogenBondAcceptors: 10,
        maxPartitionCoefficient: 5,
        setAdaptive: true,
        setAdaptiveMutation: true,
        setSelective: false,
        minQED: 0,
        minBBB: 0,
        smartsFiltering: "",
        name: "",
        alignFast: true,
        recOrder: [],
        anchorOrder: [],
        setPrepareReceptor: true,
        setFillGen: false,
        deleteInvalid: true
    };

    // Define the properties of reaction files.
    $scope.reactionFiles = {
        wrongExtension: false,
        pristine: true,
        hasFile: false,
        files: [],
        permittedExtensions: ["mrv"]
    };
    $scope.receptorFile = {
        wrongExtension: false,
        pristine: true,
        hasFile: false,
        files: [],
        permittedExtensions: ["pdb"]
    };
    $scope.anchorFragmentFile = {
        wrongExtension: false,
        pristine: true,
        hasFile: false,
        files: [],
        permittedExtensions: ["sdf"]
    };
    $scope.reactantFiles = {
        wrongExtension: false,
        pristine: true,
        hasFile: false,
        files: [],
        permittedExtensions: ["smiles", "smi"]
    };

    $scope.response = {hasError: false};

    $scope.reactantsMappingMultiSelectSettings = {checkBoxes: true, displayProp: 'name'};

    let scoreDistributionChart = null;
    let speciesDistributionChart = null;
    let evolveStatus = null;
    let species = [];
    let smilesDrawer = new SmilesDrawer.Drawer({width: 300, height: 200});
    let marvinSketcherInstance;
    let updatefailurecounter = 0;

    function getProgressUpdate(handleData) {
        jQuery.ajax({
            method: 'POST',
            type: 'POST',
            url: './progress.update',
            responseType: "application/json",
            success: function (data, textStatus, jqXHR) {
                // log data to the console so we can see
                console.log(data);
                handleData(data);
                updatefailurecounter = 0;
            },
            error: function (jqXHR, textStatus, errorThrown) {
                // Check which error was thrown
                // If progress was stopped due to an exception set big error
                // Reset form fields and output error to the page
                if (jqXHR.status === 0 && updatefailurecounter < 12) {
                    updatefailurecounter += 1;
                } else {
                    updatefailurecounter = 0;
                    getErrorResponse(jqXHR);
                    setPristine();
                    $scope.response.hasError = true;
                    $scope.$apply();
                }
            }
        });
    }

    angular.element(document).ready(function () {
        getProgressUpdate(function (jsonData) {
            if (jsonData == null) {
                return;
            }
            initializeChart(true);
            evolveStatus = jsonData.status;
            handleGenerationCollection(jsonData.generations);

            if (evolveStatus === "RUNNING") {
                handleGenerationCollection(jsonData.generationBuffer);
                setFrequentUpdateInterval()
            } else if (evolveStatus === "FAILED") {
                // Post fail message
            } else if (evolveStatus === "SUCCESS") {
                // Post success message
            }
        });
        // MarvinJSUtil.getEditor("#sketch").then(function (sketcherInstance) {
        //     marvinSketcherInstance = sketcherInstance
        // }, function (error) {
        //     alert("Loading of the sketcher failed" + error)
        // });
    });

    /**
     * Sets the form to pristine: set grey colours and such.
     */
    function setPristine() {
        $scope.compoundEvolverForm.$setPristine();
        $scope.reactionFiles.pristine = true;
        $scope.reactantFiles.pristine = true;
        $scope.receptorFile.pristine = true;
        $scope.anchorFragmentFile.pristine = true;
    }

    /**
     * Sets an effective help message for a failed http POST call
     * @param jqXHR
     */
    function getErrorResponse(jqXHR) {
        console.log(jqXHR);
        const ct = jqXHR.getResponseHeader("content-type") || "";

        // Set the error message if the response was in json
        if (ct.indexOf('json') > -1 && jqXHR.responseJSON !== undefined && jqXHR.responseJSON !== "") {
            let exception = jqXHR.responseJSON;
            if ("offspringRejectionMessages" in exception) {
                $scope.response.error = exception.message + " " + exception.offspringRejectionMessages.toString()
            } else {
                $scope.response.error = exception.message;
            }
        } else if (jqXHR.status === 0) {
            //ignore this, it breaks stuff
            $scope.response.error = "An error has occurred: the connection has failed (" + jqXHR.status + ")"
        } else {
            $scope.response.error = "An error has occurred: " + jqXHR.statusText + " (" + jqXHR.status + ")";
        }
    }

    /**
     * Extracts the form data from the form so that it is ready for posting.
     */
    function extractFormData() {
        let fileOrder = [];
        let recOrder = [];
        let anchorOrder = [];
        $scope.reactionFiles.files.forEach(function (file, i) {
            let reactantFiles = [];
            if (file.reactants.length !== 0) {
                file.reactants.forEach(function (value) {
                    reactantFiles.push(value.id);
                });
            }
            fileOrder.push(reactantFiles);
        });

        $scope.formModel.recOrder.forEach(function(value) {
            recOrder.push(value.id)
        });

        $scope.formModel.anchorOrder.forEach(function(value) {
            anchorOrder.push(value.id)
        });

        // Get form
        let form = $('form')[0];

        // Create an FormData object
        let formData = new FormData(form);
        // Append the file order as form data to the existing data
        formData.append("fileOrder", JSON.stringify(fileOrder));
        formData.append("recOrder", JSON.stringify(recOrder));
        formData.append("anchorOrder", JSON.stringify(anchorOrder));
        return formData;
    }

    function addDatasets(chart, datasets) {
        let backgroundColors = palette('cb-Set2', datasets.length).map(function (hex) {
            return '#' + hex;
        });
        datasets.forEach(function (dataset, i) {
            let species = {
                label: dataset,
                fill: true,
                backgroundColor: backgroundColors[i]
            };
            chart.data.datasets.push(species);
        });
        chart.update();
    }

    function updateSpeciesDistributionChart(generation) {
        if (speciesDistributionChart.data.datasets.length === 0) {
            species = generation.candidateList.map(item => item.species)
                .sort().filter((x, i, a) => a.indexOf(x) === i);
            addDatasets(speciesDistributionChart, species)
        }
        let speciesArray = generation.candidateList.map(item => item.species);
        let counts = {};
        for (const key of species) {
            counts[key] = 0;
        }
        speciesArray.sort().forEach(function (species) {
            counts[species] = counts[species] ? counts[species] + 1 : 1;
        });

        addData(speciesDistributionChart, generation.number, Object.values(counts));
    }

    $scope.getReactantNames = function (reactionFile) {
        let names = [];
        reactionFile.reactants.forEach(function (reactant) {
            names.push(reactant.name);
        });
        return names.join(", ");
    };

    $scope.getReceptorNames = function() {
        let names = [];
        $scope.formModel.recOrder.forEach(function (rec) {
            names.push(rec.name);
        });
        return names.join(", ");
    };

    $scope.getAnchorNames = function() {
        let names = [];
        $scope.formModel.anchorOrder.forEach(function (anchor) {
            names.push(anchor.name);
        });
        return names.join(", ");
    };
    function handleGenerationCollection(generations) {
        generations.forEach(function (generation) {

            addData(scoreDistributionChart, generation.number, [
                generation.candidateList.map(candidate => candidate.rawScore),
                generation.candidateList.map(candidate => candidate.ligandEfficiency * 10),
                generation.candidateList.map(candidate => candidate.ligandLipophilicityEfficiency)
            ]);

            if (speciesDistributionChart != null) {
                updateSpeciesDistributionChart(generation);
            }

            $rootScope.generations.push(generation);
            $rootScope.$apply();
        });
    }

    $scope.getProgressUpdate = function () {
        getProgressUpdate(function (jsonData) {
            if (jsonData == null) {
                return;
            }
            // log data to the console so we can see
            console.log(jsonData);

            let generations = jsonData.generationBuffer;
            evolveStatus = jsonData.status;

            handleGenerationCollection(generations);
        });
    };

    /**
     * Stops evolution after the current generation
     */
    $scope.terminateEvolution = function () {
        $.post("./evolution.terminate", function () {
            console.log("terminating evolution");
        })
            .done(function () {
                alert("Terminating after current generation...");
            })
            .fail(function () {
                alert("Failed to terminate evolution");
            });
    };

    function addData(chart, label, data) {
        chart.data.labels.push(label);
        chart.data.datasets.forEach(function (dataset, datasetIndex) {
            dataset.data.push(data[datasetIndex]);
        });
        chart.update();
    }

    function initializeSpeciesDistributionChart() {
        let speciesCtx = document.getElementById("species-distribution-chart").getContext("2d");
        speciesCtx.height = 512;

        let options = {
            responsive: true,
            legend: {
                position: 'top',
            },
            title: {
                display: true,
                text: 'distribution of populations'
            },
            scales: {
                yAxes: [{
                    stacked: true
                }]
            }
        };

        speciesDistributionChart = new Chart(speciesCtx, {
            type: 'line',
            options: options,
            data: {
                labels: [],
                datasets: []
            }
        })
    }

    /**
     * Initializes the chart containing generation data
     * @param multipleSpecies
     */
    function initializeChart(multipleSpecies) {
        let chartData = {
            labels: [],
            datasets: [
                {
                    data: [],
                    label: "Raw Scores",
                    borderColor: "#000000",
                    fill: "false",
                    backgroundColor: 'rgba(0,0,0,0)',
                    itemRadius: 2,
                    itemBackgroundColor: 'rgba(255,0,0,0.64)'
                },
                {
                    data: [],
                    label: "Ligand Efficiencies",
                    borderColor: "#0055ff",
                    fill: "false",
                    backgroundColor: 'rgba(0,0,0,0)',
                    itemRadius: 2,
                    itemBackgroundColor: 'rgba(255,0,0,0.64)'
                },
                {
                    data: [],
                    label: "Lipophilic Efficiencies",
                    borderColor: "#ff0055",
                    fill: "false",
                    backgroundColor: 'rgba(0,0,0,0)',
                    itemRadius: 2,
                    itemBackgroundColor: 'rgba(255,0,0,0.64)'
                }
            ]
        };

        let scoreCtx = document.getElementById("score-distribution-chart").getContext('2d');
        scoreCtx.height = 512;

        if (scoreDistributionChart !== null) {
            scoreDistributionChart.destroy();
        }

        scoreDistributionChart = new Chart(scoreCtx, {
            type: 'violin',
            data: chartData,
            options: {
                responsive: true,
                legend: {
                    position: 'top',
                },
                title: {
                    display: true,
                    text: 'fitness of populations'
                },
                onClick: chartClickEvent
            }
        });

        if (multipleSpecies) {
            initializeSpeciesDistributionChart();
        }
    }

    /**
     * Runs when the chart containing generation data is clicked
     * @param event
     * @param array
     */
    function chartClickEvent(event, array) {
        if (scoreDistributionChart === 'undefined' || scoreDistributionChart == null) {
            return;
        }
        if (event === 'undefined' || event == null) {
            return;
        }
        if (array === 'undefined' || array == null) {
            return;
        }
        if (array.length <= 0) {
            return;
        }

        let elementIndex = 0;

        $rootScope.selectedGenerationNumber = array[elementIndex]['_index'];
        $rootScope.$apply();
        $scope.runVisualization();
    }

    function setFrequentUpdateInterval() {
        document.getElementById("loader").hidden = false;
        let i = setInterval(function () {
            // do your thing
            $scope.getProgressUpdate();
            //console.log(evolveStatus);
            if (["FAILED", "SUCCESS"].includes(evolveStatus)) {
                document.getElementById("loader").hidden = true;
                clearInterval(i);
            }
        }, 5000);
    }

    /**
     * Gets called when the submit button is clicked
     * @param valid Boolean true when the form is valid
     */
    $scope.onSubmit = function (valid) {
        if (valid) {
            // Reset response error status to remove the warning box
            $scope.response.hasError = false;
            $rootScope.generations = [];

            let formData = extractFormData();

            // process the form
            jQuery.ajax({
                method: 'POST',
                type: 'POST',
                url: './evolve.do',
                processData: false,
                contentType: false,
                responseType: "application/json",
                data: formData,
                success: function (data, textStatus, jqXHR) {

                    // log data to the console so we can see
                    console.log(data);
                    $scope.$apply();
                },
                error: function (jqXHR, textStatus, errorThrown) {

                    // Reset form fields and output error to the page
                    getErrorResponse(jqXHR);
                    setPristine();
                    $scope.response.hasError = true;
                    $scope.$apply();
                    evolveStatus = "FAILED";
                }
            });
            initializeChart($scope.reactionFiles.files.length > 1);

            evolveStatus = null;

            setFrequentUpdateInterval();

        } else {
            // Form is not valid. Keep quiet.
            console.log("Invalid Form!");
        }
    };

    /**
     * Get the population of the selected generation, or nothing if there is no selected generation
     * @returns {Array|*}
     */
    $scope.getPopulation = function () {
        if ($scope.hasData() && $scope.generationSelected()) {
            return $rootScope.generations[$rootScope.selectedGenerationNumber].candidateList;
        }
        return [];
    };

    /**
     * Gets the compound with the highest fitness
     * @param generation the generation to get the compound from
     * @returns the compound with the highest fitness
     */
    $scope.getMostFitCompound = function (generation) {
        let candidates = generation.candidateList;
        return candidates.reduce(function (l, e) {
            return e.fitness > l.fitness ? e : l;
        });
    };

    /**
     * Generic file downloading function
     * @param url the url to download data from
     */
    function download(url) {
        let iframe = document.createElement("iframe");
        iframe.setAttribute("src", url);
        iframe.setAttribute("style", "display: none");
        document.body.appendChild(iframe);
    }

    /**
     * Downloads a zip for a specific compound
     * @param compoundId the compound data to download
     */
    $scope.downloadCompound = function (compoundId) {
        // Set data
        let url = './compound.download?compoundId=' + compoundId;

        download(url);
    };

    /**
     * Downloads a csv file containing scores for each generation
     */
    $scope.downloadCsv = function () {
        // Set data
        let url = './csv.download';

        download(url);
    };

    /**
     * Is there data on generations?
     * @returns {boolean}
     */
    $scope.hasData = function () {
        return $rootScope.generations.length > 0;
    };

    $scope.generationSelected = function () {
        return $rootScope.selectedGenerationNumber != null;
    };

    $scope.downloadRun = function () {
        // Get the url without parameters: get the entire run.
        let url = './compound.download';

        download(url);
    };

    $scope.downloadSelectedGeneration = function () {
        // Set data
        let url = './multi-sdf.download?generationNumber=' + $rootScope.selectedGenerationNumber;
        console.log(url);

        download(url);
    };

    $scope.downloadMultiSdf = function (bestOnly) {
        // Set data
        console.log(bestOnly);
        let url = './multi-sdf.download' + (bestOnly ? '?bestOnly=True' : '');
        console.log(url);

        download(url);
    };

    $scope.showSmiles = function (smiles, canvas_name) {
        SmilesDrawer.parse(smiles, function (tree) {
            // Draw to the canvas
            smilesDrawer.draw(tree, canvas_name.toString(), 'light', false);
        });
    };

    $scope.doMarvin = function () {
        let reactionFiles = document.getElementById("reaction-files");
        marvinSketcherInstance.exportStructure("mrv", {}).then(function (a) {
                console.log(a);
                const dT = new ClipboardEvent('').clipboardData || // Firefox < 62 workaround exploiting https://bugzilla.mozilla.org/show_bug.cgi?id=1422655
                    new DataTransfer(); // specs compliant (as of March 2018 only Chrome)
                let f = new File([a.toString()], "marvin.mrv");
                dT.items.add(f);
                for (let i = 0; i < reactionFiles.files.length; i++) {
                    dT.items.add(reactionFiles.files[i])
                }

                reactionFiles.files = dT.files;
                // names = [];
                // console.log(JSON.stringify(reactionFiles.files));
                // for (let i = 0; i < reactionFiles.files.length; i++) {
                //     names.push(reactionFiles.files[i].name);
                // }
                // console.log(JSON.stringify(names));
            }
        );

        // document.getElementById('reactions-label').innerText = names.join(", ")
    }

});