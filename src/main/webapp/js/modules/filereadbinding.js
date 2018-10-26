angular.module('fileReadBinding', []).directive("fileBind", ['$parse', function ($parse) {
    return {
        restrict: 'A',
        link: function (scope, element, attributes) {

            /**
             * Updates variables that describe the state of the reactionFile input
             */
            function updateFileParameters() {
                $parse(attributes.fileBind)
                    .assign(scope, element);

                var requiredExtensions;
                if (attributes.name === "reactionFile") {
                    requiredExtensions = ["mrv"];
                } else if (attributes.name === "reactantFiles") {
                    requiredExtensions = ["smi", "smiles"]
                }

                var extension;

                if (attributes.multiple !== undefined) {
                    let invalidityArray = [];
                    let names = [];
                    Array.prototype.forEach.call(element[0].files, file => {
                        extension = file.name.split(".").pop();
                        let wrongExtension = requiredExtensions.indexOf(extension) === -1;
                        invalidityArray.push(wrongExtension);
                        names.push(file.name)
                    });
                    scope[attributes.name].names = names;
                    scope[attributes.name].wrongExtension = (invalidityArray.indexOf(true) !== -1)

                } else {
                    extension = element[0].files[0].name.split(".").pop();
                    scope[attributes.name].wrongExtension = requiredExtensions.indexOf(extension) === -1;
                }

            }

            // Update when the element is touched
            element.bind("change", function () {
                updateFileParameters();
                scope[attributes.name].pristine = false;
                scope[attributes.name].hasFile = true;
                scope.$apply()
            });

            // When check for files existing in the input when the directive loads
            if (element.get(0).files.length !== 0) {
                scope[attributes.name].hasFile = true;
                updateFileParameters();
            }
        }
    }
}]);