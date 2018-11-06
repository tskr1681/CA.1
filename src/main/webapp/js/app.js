var app = angular.module('compoundEvolver', ['fileReadBinding']);

app.run(function ($rootScope) {
    $rootScope.app = {hasData:false};
    // $rootScope.generations = [{number:1, mostFitCompound: {iupacName:"2-(1H-indol-3-yl)ethan-1-amine", bb:"other", fitness:-7.43}}];
    $rootScope.generations = [{number:0, candidateList: [{iupacName: "2-(1H-indol-3-yl)ethan-1-amine", fitness:-7.43}]}];
});

app.controller('FormInputCtrl' , function ($scope, $rootScope) {
    $scope.formModel = {
        generationSize: 50,
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
        forceField: 'mmff94',
        useLipinski: false,
        maxMolecularMass: 500,
        maxHydrogenBondDonors: 5,
        maxHydrogenBondAcceptors: 10,
        maxPartitionCoefficient: 5
    };
    $scope.reactionFile = {wrongExtension: false, pristine: true, hasFile: false, permittedExtensions: ["mrv"]};
    $scope.receptorFile = {wrongExtension: false, pristine: true, hasFile: false, permittedExtensions: ["mab"]};
    $scope.anchorFragmentFile = {wrongExtension: false, pristine: true, hasFile: false, permittedExtensions: ["sdf"]};
    $scope.reactantFiles = {wrongExtension: false, pristine: true, hasFile: false, files:[], permittedExtensions: ["smiles", "smi"]};

    $scope.response = {hasError: false};

    var myChart = null;

    $( function() {
        $("#sortable").sortable();
    });

    /**
     * Sets the form to pristine: set grey colours and such.
     */
    function setPristine() {
        $scope.compoundEvolverForm.$setPristine();
        $scope.reactionFile.pristine = true;
        $scope.reactionFile.hasFile = false;
        $scope.reactantFiles.pristine = true;
        $scope.reactantFiles.hasFile = false;
        $scope.receptorFile.pristine = true;
        $scope.receptorFile.hasFile = false;
        $scope.anchorFragmentFile.pristine = true;
        $scope.anchorFragmentFile.hasFile = false;
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
            let jsonData = $.parseJSON(jqXHR.responseJSON);

            if (jqXHR.responseJSON !== undefined && jqXHR.responseJSON !== "") {
                $scope.response.error = jsonData.fieldName + ": "+ jsonData.cause
            } else if (jqXHR.status === 0) {
                $scope.response.error = "Connection failed"
            } else {
                $scope.response.error = "Generic error"
            }
        }
    }

    function extractFormData() {
        let fileOrder = [];

        $("#sortable").sortable("toArray").forEach(function (id) {
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

    function getProgressUpdate() {
        jQuery.ajax({
            method: 'POST',
            type: 'POST',
            url: './progress.update',
            processData: false,
            contentType: false,
            responseType: "application/json",
            dataType: 'json',
            success: function (data, textStatus, jqXHR) {
                // log data to the console so we can see
                console.log(data);
                // update variables with new data
                function getSum(total, num) {
                    return total + num;
                }

                data.forEach(function (generation) {
                    generationFitnesses = generation.candidateList.map(candidate => candidate.fitness);
                    addData(myChart, generation.number, [
                        generationFitnesses.reduce(getSum) / generationFitnesses.length,
                        Math.min.apply(Math, generationFitnesses),
                        Math.max.apply(Math, generationFitnesses)
                    ]);
                    $rootScope.generations.push(generation);
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
    }

    function addData(chart, label, data) {
        chart.data.labels.push(label);
        console.log(data);
        data.forEach(function (value, datasetIndex) {
            console.log(value, datasetIndex);
            chart.data.datasets[datasetIndex].data.push(value);
        });
        chart.update();
    }

    function initializeChart(avgScores, minScores, maxScores) {
        var chartData = {labels: [...Array(avgScores.length).keys()],
            datasets: [
                {data: avgScores, label: "average", borderColor: "#000000", fill: "false"},
                {data: minScores, label: "minimum", borderColor: "#00ffaa", fill: "false"},
                {data: maxScores, label: "maximum", borderColor: "#ff0055", fill: "false"}]};

        var ctx = document.getElementById("myChart").getContext('2d');

        if (myChart !== null) {myChart.destroy();}

        myChart = new Chart(ctx, {
            type: 'line',
            data: chartData,
        });
    }

    /**
     * Gets called when the submit button is clicked
     * @param valid Boolean true when the form is valid
     */
    $scope.onSubmit = function (valid) {
        if (valid) {
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
                    // function getSum(total, num) {
                    //     return total + num;
                    // }
                    // var avgScores = data.map(arr => arr.reduce(getSum) / arr.length);
                    // var maxScores = data.map(arr => Math.max.apply(Math, arr));
                    // var minScores = data.map(arr => Math.min.apply(Math, arr));
                    // initializeChart(minScores, avgScores, maxScores);
                    $scope.response.hasError = false;
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
            initializeChart([], [], []);

            var counter = 0;
            var i = setInterval(function(){
                // do your thing
                getProgressUpdate();
                counter++;
                if(counter === 1000) {
                    clearInterval(i);
                }
            }, 2000);

        } else {
            // Form is not valid. Keep quiet.
            console.log("Invalid Form!");
        }
    }

});

app.controller('CompoundsCtrl', function ($scope, $rootScope, $sce) {
    $scope.getGenerations = function() {
        return $rootScope.generations
    };

    $scope.getMostFitCompound = function (generation) {
        let candidates = generation.candidateList;
        return candidates.reduce(function (l, e) {
            return e.fitness > l.fitness ? e : l;
        });
    };

});