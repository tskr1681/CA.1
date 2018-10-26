var app = angular.module('compoundEvolver', ['fileReadBinding']);

app.run(function ($rootScope) {
    $rootScope.app = {hasSummary: false, hasProbes:false};
    $rootScope.summary = {};
    $rootScope.probes = {};
});

app.controller('FormInputCtrl' , function ($scope, $rootScope) {
    $scope.formModel = {
        generationSize: 50,
        numberOfGenerations: 20,
        selectionSize: 0.4,
        mutationRate: 0.5,
        crossoverRate: 0.8,
        elitistRate: 0.1,
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
    $scope.reactionFile = {wrongExtension: false, pristine: true, hasFile: false};
    $scope.reactantFiles = {wrongExtension: false, pristine: true, hasFile: false, names: ""};
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
        $scope.reactionFile.hasFile = true;
        $scope.reactantFiles.pristine = true;
        $scope.reactantFiles.pristine = true;
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

    /**
     * Gets called when the submit button is clicked
     * @param valid Boolean true when the form is valid
     */
    $scope.onSubmit = function (valid) {
        if (valid) {

            var fileOrder = [];

            $("#sortable").sortable("toArray").forEach(function (id) {
                let splitted_id = id.split("-");
                fileOrder.push(splitted_id[splitted_id.length - 1]);
            });

            console.log(fileOrder);

            var form = $('form')[0];

            // Create an FormData object
            var formData = new FormData(form);
            formData.append("fileOrder", JSON.stringify(fileOrder));

            console.log(formData);

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
                    function getSum(total, num) {
                        return total + num;
                    }
                    var avgScores = data.map(arr => arr.reduce(getSum) / arr.length);
                    var maxScores = data.map(arr => Math.max.apply(Math, arr));
                    var minScores = data.map(arr => Math.min.apply(Math, arr));
                    console.log(minScores, maxScores);
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
            })
        } else {
            // Form is not valid. Keep quiet.
            console.log("Invalid Form!");
        }
    }
});