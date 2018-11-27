var app = angular.module('compoundEvolver', ['fileReadBinding']);

app.run(function ($rootScope) {
    $rootScope.app = {hasData:false};
    // $rootScope.generations = [{number:1, mostFitCompound: {iupacName:"2-(1H-indol-3-yl)ethan-1-amine", bb:"other", fitness:-7.43}}];
    $rootScope.generations = [];
    $rootScope.selectedGenerationNumber = null;
});

app.controller('FormInputCtrl' , function ($scope, $rootScope) {
    $scope.formModel = {
        generationSize: 16,
        numberOfGenerations: 20,
        selectionSize: 0.4,
        mutationRate: 0.1,
        crossoverRate: 0.8,
        elitismRate: 0.1,
        randomImmigrantRate:0.1,
        selectionMethod:'Fitness proportionate selection',
        mutationMethod:'Distance dependent',
        terminationCondition: 'fixed',
        nonImprovingGenerationQuantity: 0.3,
        conformerCount: 15,
        fitnessMeasure: 'ligandEfficiency',
        forceField: 'smina',
        useLipinski: false,
        maxMolecularMass: 500,
        maxHydrogenBondDonors: 5,
        maxHydrogenBondAcceptors: 10,
        maxPartitionCoefficient: 5
    };
    $scope.reactionFile = {wrongExtension: false, pristine: true, hasFile: false, permittedExtensions: ["mrv"]};
    $scope.receptorFile = {wrongExtension: false, pristine: true, hasFile: false, permittedExtensions: ["pdb"]};
    $scope.anchorFragmentFile = {wrongExtension: false, pristine: true, hasFile: false, permittedExtensions: ["sdf"]};
    $scope.reactantFiles = {wrongExtension: false, pristine: true, hasFile: false, files:[], permittedExtensions: ["smiles", "smi"]};

    $scope.response = {hasError: false};

    var myChart = null;
    var evolveStatus = null;

    $( function() {
        $("#sortableReactantList").sortable({
            handle: 'span',
            cursor: 'grabbing'
        }).disableSelection();
    });

    /**
     * Sets the form to pristine: set grey colours and such.
     */
    function setPristine() {
        $scope.compoundEvolverForm.$setPristine();
        $scope.reactionFile.pristine = true;
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

        $("#sortableReactantList").sortable("toArray").forEach(function (id) {
            let splitted_id = id.split("-");
            fileOrder.push(splitted_id[splitted_id.length - 1]);
        });

        // Get form
        let form = $('form')[0];

        // Create an FormData object
        let formData = new FormData(form);
        // Append the file order as form data to the existing data
        formData.append("fileOrder", JSON.stringify(fileOrder));
        return formData;
    }

    $scope.getProgressUpdate = function() {
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
                    generationFitnesses = generation.candidateList.map(candidate => candidate.ligandEfficiency);
                    addData(myChart, generation.number, [
                        generationFitnesses
                    ]);
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
        console.log(data);
        data.forEach(function (value, datasetIndex) {
            console.log(value, datasetIndex);
            chart.data.datasets[datasetIndex].data.push(value);
        });
        chart.update();
    }

    function initializeChart(scores) {
        var chartData = {labels: [...Array(scores.length).keys()],
            datasets: [
                {data: scores, label: "fitness", borderColor: "#000000", fill: "false", backgroundColor: 'rgba(0,0,0,0)', itemRadius: 2, itemBackgroundColor: 'rgba(255,0,0,0.64)'},]};

        var ctx = document.getElementById("myChart").getContext('2d');

        if (myChart !== null) {myChart.destroy();}

        myChart = new Chart(ctx, {
            type: 'boxplot',
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
    }

    function chartClickEvent(event, array){
        if (myChart === 'undefined' || myChart == null)
        {
            return;
        }
        if (event === 'undefined' || event == null)
        {
            return;
        }
        if (array === 'undefined' || array == null)
        {
            return;
        }
        if (array.length <= 0)
        {
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
            initializeChart([]);

            evolveStatus = null;

            var i = setInterval(function(){
                // do your thing
                $scope.getProgressUpdate();
                if(["FAILED", "SUCCESS"].includes(evolveStatus)) {
                    clearInterval(i);
                }
            }, 5000);

        } else {
            // Form is not valid. Keep quiet.
            console.log("Invalid Form!");
        }
    }

});

app.controller('CompoundsCtrl', function ($scope, $rootScope, $sce) {
    $scope.getPopulation = function() {
        return $rootScope.generations[$rootScope.selectedGenerationNumber].candidateList;
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
        let url = './csv.download?generationNumber=' + $rootScope.selectedGenerationNumber;

        download(url);
    };
});