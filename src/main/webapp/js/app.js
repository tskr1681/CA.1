var app = angular.module('compoundEvolver', ['fileReadBinding']);

app.run(function ($rootScope) {
    $rootScope.app = {hasSummary: false, hasProbes:false};
    $rootScope.summary = {};
    $rootScope.probes = {};
});

app.controller('FormInputCtrl' , function ($scope, $rootScope) {
    $scope.formModel = {generationSize: 50, mutationRate: 0.5, crossoverRate: 0.5};
    $scope.file = {wrongExtension: false, pristine: true, hasFile: false};
    $scope.response = {hasError: false};

    /**
     * Sets the form to pristine: set grey colours and such.
     */
    function setPristine() {
        $scope.compoundEvolverForm.$setPristine();
        $scope.file.pristine = true;
        $scope.file.hasFile = true;
    }

    /**
     * Sets an effective help message for a failed http POST call
     * @param jqXHR
     */
    function getErrorResponse(jqXHR) {
        if (jqXHR.responseText !== undefined && jqXHR.responseText !== "") {
            $scope.response.error = jqXHR.responseText.substr(1).slice(0, -1);
        } else if (jqXHR.status === 0) {
            $scope.response.error = "Connection failed"
        } else {
            $scope.response.error = "Generic error"
        }
    }

    /**
     * Gets called when the submit button is clicked
     * @param valid Boolean true when the form is valid
     */
    $scope.onSubmit = function (valid) {
        if (valid) {

            var form = $('form')[0];

            // Create an FormData object
            var formData = new FormData(form);

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
            console.log("Invalid Form!")
        }
    }
});