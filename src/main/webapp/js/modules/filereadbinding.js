angular.module('fileReadBinding', []).directive("fileBind", ['$parse', function ($parse) {
    return {
        restrict: 'A',
        link: function (scope, element, attributes) {

            /**
             * Updates variables that describe the state of the file input
             */
            function updateFileParameters() {
                $parse(attributes.fileBind)
                    .assign(scope, element);

                var extn = element[0].files[0].name.split(".").pop();
                scope.file.wrongExtension = ["mrv"].indexOf(extn) === -1;
            }

            element.bind("change", function () {
                updateFileParameters();
                scope.file.pristine = false;
                scope.file.hasFile = true;
                scope.$apply()
            });

            if (element.get(0).files.length !== 0) {
                scope.file.hasFile = true;
                updateFileParameters();
            }
        }
    }
}]);