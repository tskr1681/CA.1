var app = angular.module('compoundEvolver', ['fileReadBinding', 'angularjs-dropdown-multiselect']);

app.run(function ($rootScope) {
    // $rootScope.generations = [{number:1, mostFitCompound: {iupacName:"2-(1H-indol-3-yl)ethan-1-amine", bb:"other", fitness:-7.43}}];
    $rootScope.generations = [];
    $rootScope.selectedGenerationNumber = null;
});

app.directive('multiselectDropdown', [function() {
    return function(scope, element, attributes) {

        element = $(element[0]); // Get the element as a jQuery element

        // Below setup the dropdown:

        element.selectpicker();

        // Below maybe some additional setup
    }
}]);

app.controller('FormInputCtrl', function ($scope, $rootScope) {
    $scope.formModel = {
        generationSize: 16,
        numberOfGenerations: 20,
        selectionSize: 0.4,
        mutationRate: 0.1,
        crossoverRate: 0.8,
        elitismRate: 0.1,
        randomImmigrantRate: 0.1,
        selectionMethod: 'Fitness proportionate selection',
        mutationMethod: 'Distance dependent',
        terminationCondition: 'fixed',
        nonImprovingGenerationQuantity: 0.3,
        conformerCount: 15,
        fitnessMeasure: 'ligandEfficiency',
        forceField: 'smina',
        maxAnchorMinimizedRmsd: 2,
        useLipinski: false,
        maxMolecularMass: 500,
        maxHydrogenBondDonors: 5,
        maxHydrogenBondAcceptors: 10,
        maxPartitionCoefficient: 5
    };
    $scope.reactionFiles = {
        wrongExtension: false,
        pristine: true,
        hasFile: false,
        files: [],
        permittedExtensions: ["mrv"]
    };
    $scope.receptorFile = {wrongExtension: false, pristine: true, hasFile: false, permittedExtensions: ["pdb"]};
    $scope.anchorFragmentFile = {wrongExtension: false, pristine: true, hasFile: false, permittedExtensions: ["sdf"]};
    $scope.reactantFiles = {
        wrongExtension: false,
        pristine: true,
        hasFile: false,
        files: [],
        permittedExtensions: ["smiles", "smi"]
    };

    $scope.response = {hasError: false};

    $scope.reactantsMappingMultiSelectSettings = { checkBoxes: true, displayProp: 'name'};

    var scoreDistributionChart = null;
    var speciesDistributionChart = null;
    var evolveStatus = null;
    var orderCount = 0;
    var species = [];

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
        let ct = jqXHR.getResponseHeader("content-type") || "";
        if (ct.indexOf('html') > -1) {
            if (jqXHR.responseText !== undefined && jqXHR.responseText !== "") {
                $scope.response.error = jqXHR.responseText.substr(1).slice(0, -1);
            } else if (jqXHR.status === 0) {
                $scope.response.error = "Connection failed"
            } else {
                $scope.response.error = "Generic error"
            }
        }
        if (ct.indexOf('json') > -1) {
            console.log(jqXHR.responseJSON);
            let exception = jqXHR.responseJSON;

            if (jqXHR.responseJSON !== undefined && jqXHR.responseJSON !== "") {
                // $scope.response.error = exception.fieldName + ": "+ exception.cause
                console.log(exception);
                if ("offspringRejectionMessages" in exception) {
                    $scope.response.error = exception.message + " " + exception.offspringRejectionMessages.toString()
                } else {
                    console.log(exception.message);
                    $scope.response.error = exception.message;
                }
            } else if (jqXHR.status === 0) {
                $scope.response.error = "Connection failed"
            } else {
                $scope.response.error = "Generic error"
            }
        }
    }

    function extractFormData() {
        let fileOrder = [];

        $scope.reactionFiles.files.forEach(function (file, i) {
            let reactantFiles = [];
            if (file.reactants.length !== 0) {
                file.reactants.forEach(function (value) {
                    reactantFiles.push(value.id);
                });
            }
            fileOrder.push(reactantFiles);
        });
        console.log(fileOrder);

        // Get form
        let form = $('form')[0];

        // Create an FormData object
        let formData = new FormData(form);
        // Append the file order as form data to the existing data
        formData.append("fileOrder", JSON.stringify(fileOrder));
        return formData;
    }

    function addDatasets(chart, datasets) {
        let backgroundColors = palette('cb-Set2', datasets.length).map(function(hex) {
            return '#' + hex;
        });
        console.log(backgroundColors);
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
        console.log(speciesDistributionChart.data);
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

    $scope.getReactantNames = function(reactionFile) {
        let names = [];
        reactionFile.reactants.forEach( function (reactant) {
            names.push(reactant.name);
        });
        return names.join(", ");
    };

    $scope.getProgressUpdate = function () {
        jQuery.ajax({
            method: 'POST',
            type: 'POST',
            url: './progress.update',
            responseType: "application/json",
            success: function (data, textStatus, jqXHR) {
                // log data to the console so we can see
                console.log(data);

                // update variables with new data
                function getSum(total, num) {
                    return total + num;
                }

                let generations = data.generationBuffer;
                evolveStatus = data.status;

                generations.forEach(function (generation) {

                    addData(scoreDistributionChart, generation.number, [
                        generation.candidateList.map(candidate => candidate.rawScore),
                        generation.candidateList.map(candidate => candidate.ligandEfficiency),
                        generation.candidateList.map(candidate => candidate.ligandLipophilicityEfficiency)
                    ]);

                    updateSpeciesDistributionChart(generation);

                    $rootScope.generations.push(generation);
                    $rootScope.$apply();
                });

            },
            error: function (jqXHR, textStatus, errorThrown) {
                // Check which error was thrown
                // If progress was stopped due to an exception set big error
                // Reset form fields and output error to the page
                getErrorResponse(jqXHR);
                setPristine();
                $scope.response.hasError = true;
                $scope.$apply();
            }
        })
    };

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
        var speciesCtx = document.getElementById("species-distribution-chart").getContext("2d");

        var options = {
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

    function initializeChart() {
        var chartData = {
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

        var scoreCtx = document.getElementById("score-distribution-chart").getContext('2d');

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

        initializeSpeciesDistributionChart();
    }

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

        var elementIndex = 0;

        var chartData = array[elementIndex]['_chart'].config.data;
        var idx = array[elementIndex]['_index'];

        var label = chartData.labels[idx];
        var value = chartData.datasets[elementIndex].data[idx];
        var series = chartData.datasets[elementIndex].label;

        $rootScope.selectedGenerationNumber = idx;
        $rootScope.$apply();
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

            var formData = extractFormData();

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
                }
            });
            initializeChart();

            evolveStatus = null;

            var i = setInterval(function () {
                // do your thing
                $scope.getProgressUpdate();
                if (["FAILED", "SUCCESS"].includes(evolveStatus)) {
                    clearInterval(i);
                }
            }, 5000);

        } else {
            // Form is not valid. Keep quiet.
            console.log("Invalid Form!");
        }
    };

    $scope.getPopulation = function () {
        if ($scope.hasData() && $scope.generationSelected()) {
            return $rootScope.generations[$rootScope.selectedGenerationNumber].candidateList;
        }
        return [];
    };

    $scope.getMostFitCompound = function (generation) {
        let candidates = generation.candidateList;
        return candidates.reduce(function (l, e) {
            return e.fitness > l.fitness ? e : l;
        });
    };

    function download(url) {
        var iframe = document.createElement("iframe");
        iframe.setAttribute("src", url);
        iframe.setAttribute("style", "display: none");
        document.body.appendChild(iframe);
    }

    $scope.downloadCompound = function (compoundId) {
        // Set data
        let url = './compound.download?compoundId=' + compoundId;

        download(url);
    };

    $scope.downloadCsv = function () {
        // Set data
        let url = './csv.download';

        download(url);
    };

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
    }
});