<%--
  Copyright (c) 2018 C.A. (Robert) Warmerdam [c.a.warmerdam@st.hanze.nl].
  All rights reserved.
--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html lang="en" ng-app="compoundEvolver">
<head>
    <title>Compound Evolver Application</title>
    <%--set meta data--%>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <%--Load stylesheets--%>
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/css/bootstrap.min.css"
          integrity="sha384-MCw98/SFnGE8fJT3GXwEOngsV7Zt27NXFoaoApmYm81iuXoPkFOJwJ8ERdknLPMO"
          crossorigin="anonymous">
    <link rel="stylesheet"
          href="https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.13.2/css/bootstrap-select.min.css">
    <link rel="stylesheet" href="<c:url value = "css/main.css"/>">
    <%--load library javascript--%>
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script>
    <script src="https://code.jquery.com/ui/1.12.1/jquery-ui.min.js"
            integrity="sha256-VazP97ZCwtekAsvgPBSUwPFKdrwD3unUfSGVYrahUqU="
            crossorigin="anonymous"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.3/umd/popper.min.js"
            integrity="sha384-ZMP7rVo3mIykV+2+9J3UJ46jBk0WLaUAdn689aCwoqbBJiSnjAK/l8WvCWPIPm49"
            crossorigin="anonymous"></script>
    <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.1.3/js/bootstrap.min.js"
            integrity="sha384-ChfqqxuZUCnJSK3+MXmPNIyE6ZbWh2IMqE241rYiqJxyMiZ6OW/JmZQ5stwEULTy"
            crossorigin="anonymous"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.13.2/js/bootstrap-select.min.js"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.6.7/angular.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.7.2/Chart.min.js"></script>
    <script src="https://unpkg.com/chartjs-chart-box-and-violin-plot"></script>
    <script src="https://codepen.io/anon/pen/aWapBE.js"></script>
    <script src="<c:url value = "js/libs/angularjs-dropdown-multiselect.js"/>"></script>
    <%--load custom javascript--%>
    <script src="<c:url value = "js/app.js"/>"></script>
    <script src="<c:url value = "js/modules/filereadbinding.js"/>"></script>
</head>
<body>
<div class="container" ng-controller="FormInputCtrl">
    <div class="page-header">
        <h1>Compound Evolver Application</h1>
    </div>
    <div class="row">
        <div class="col-lg-12">
        </div>
    </div>
    <%--Setup dynamic form using angularjs--%>
    <div class="row">
        <div class="col-lg-12">
            <h2>Set genetic algorithm parameters</h2>
            <form name="compoundEvolverForm"
                  novalidate="novalidate"
                  enctype="multipart/form-data">
                <div class="alert alert-danger" ng-show="response.hasError" ng-bind="response.error"></div>
                <div class="card">
                    <h5 class="card-header"><b>Building blocks and reaction</b></h5>
                    <div id="file-input" class="card-body">
                        <div class="form-group row">
                            <label for="reaction-files" class="col-sm-3 col-form-label">Reaction files (.mrv)</label>
                            <div class="col-sm-9">
                                <div class="custom-file">
                                    <input type="file"
                                           class="custom-file-input"
                                           name="reactionFiles"
                                           id="reaction-files"
                                           required="required"
                                           multiple="multiple"
                                           file-bind="formModel.reactionFiles"
                                           ng-class="{
                    'is-invalid':reactionFiles.wrongExtension || (!reactionFiles.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)),
                    'is-valid':!reactionFiles.wrongExtension && (!reactionFiles.pristine || compoundEvolverForm.$submitted)}">
                                    <label class="custom-file-label overflow-hidden"
                                           for="reaction-files">
                                        <span ng-repeat="file in reactionFiles.files">{{file.name}} </span>
                                        <span ng-hide="reactionFiles.files.length">Choose reaction files</span>
                                    </label>
                                </div>
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="!reactionFiles.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </small>
                                <small class="form-text text-danger"
                                       ng-show="reactionFiles.wrongExtension">
                                    Only an mrv file (.mrv) is accepted
                                </small>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="reactant-files" class="col-sm-3 float-sm-left col-form-label">Reactants files
                                (.smiles,
                                .smi)</label>
                            <div class="col-sm-9 float-sm-left">
                                <div class="custom-file">
                                    <input type="file"
                                           class="custom-file-input"
                                           name="reactantFiles"
                                           id="reactant-files"
                                           required="required"
                                           multiple="multiple"
                                           file-bind="formModel.reactantFiles"
                                           ng-class="{
                    'is-invalid':reactantFiles.wrongExtension || (!reactantFiles.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)),
                    'is-valid':!reactantFiles.wrongExtension && (!reactantFiles.pristine || compoundEvolverForm.$submitted)}">
                                    <label class="custom-file-label overflow-hidden"
                                           for="reactant-files">
                                        <span ng-repeat="file in reactantFiles.files">{{file.name}} </span>
                                        <span ng-hide="reactantFiles.files.length">Choose reactant files</span>
                                    </label>
                                </div>
                            </div>
                            <small class="col-sm-9 float-sm-right form-text text-danger"
                                   ng-show="!reactantFiles.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                This field is required
                            </small>
                            <small class="col-sm-9 float-sm-right form-text text-danger"
                                   ng-show="reactantFiles.wrongExtension">
                                Only smiles files (.smiles, .smi) are accepted
                            </small>
                        </div>
                        <div class="form-group row" ng-repeat="reactionFile in reactionFiles.files track by $index">
                            <label class="col-sm-3 col-form-label">Reactants for {{reactionFile.name}}</label>
                            <div class="col-sm-9">
                                <div class="float-sm-left">
                                    <div ng-dropdown-multiselect=""
                                         options="reactantFiles.files"
                                         selected-model="reactionFile.reactants"
                                         extra-settings="reactantsMappingMultiSelectSettings">
                                    </div>
                                </div>
                                <div class="alert alert-primary float-sm-right" role="alert">
                                    {{getReactantNames(reactionFile)}}
                                </div>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="receptor-file" class="col-sm-3 col-form-label">Receptor file (.pdb)</label>
                            <div class="col-sm-9">
                                <div class="custom-file">
                                    <input type="file"
                                           class="custom-file-input"
                                           name="receptorFile"
                                           id="receptor-file"
                                           required="required"
                                           file-bind="formModel.receptorFile"
                                           ng-class="{
                    'is-invalid':receptorFile.wrongExtension || (!receptorFile.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)),
                    'is-valid':!receptorFile.wrongExtension && (!receptorFile.pristine || compoundEvolverForm.$submitted)}">
                                    <label class="custom-file-label overflow-hidden"
                                           for="receptor-file"
                                           ng-bind="formModel.receptorFile[0].files[0].name || 'Choose receptor file'"></label>
                                </div>
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="!receptorFile.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </small>
                                <small class="form-text text-danger"
                                       ng-show="receptorFile.wrongExtension">
                                    Only a mab file (.pdb) is accepted (use moloc to convert pdb to mab)
                                </small>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="anchor-fragment-file" class="col-sm-3 col-form-label">Anchor fragment file
                                (.sdf)</label>
                            <div class="col-sm-9">
                                <div class="custom-file">
                                    <input type="file"
                                           class="custom-file-input"
                                           name="anchorFragmentFile"
                                           id="anchor-fragment-file"
                                           required="required"
                                           file-bind="formModel.anchorFragmentFile"
                                           ng-class="{
                    'is-invalid':anchorFragmentFile.wrongExtension || (!anchorFragmentFile.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)),
                    'is-valid':!anchorFragmentFile.wrongExtension && (!anchorFragmentFile.pristine || compoundEvolverForm.$submitted)}">
                                    <label class="custom-file-label overflow-hidden"
                                           for="anchor-fragment-file"
                                           ng-bind="formModel.anchorFragmentFile[0].files[0].name || 'Choose anchor file'"></label>
                                </div>
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="!anchorFragmentFile.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </small>
                                <small class="form-text text-danger"
                                       ng-show="anchorFragmentFile.wrongExtension">
                                    Only an sdf file (.sdf) is accepted
                                </small>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="card">
                    <h5 class="card-header"><b>Genetic operators</b></h5>
                    <div id="operator-settings" class="card-body">
                        <div class="form-group row">
                            <label for="generation-size" class="col-sm-3 col-form-label">Size of initial
                                generation</label>
                            <div class="col-sm-9">
                                <input type="number"
                                       class="form-control"
                                       ng-model="formModel.generationSize"
                                       id="generation-size"
                                       name="generationSize"
                                       required="required"
                                       min="2"
                                       step="1"
                                       ng-class="{
                    'is-invalid':!compoundEvolverForm.generationSize.$valid && (!compoundEvolverForm.generationSize.$pristine || compoundEvolverForm.$submitted),
                    'is-valid':compoundEvolverForm.generationSize.$valid && (!compoundEvolverForm.generationSize.$pristine || compoundEvolverForm.$submitted)}">
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="compoundEvolverForm.generationSize.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </small>
                                <small class="form-text text-danger"
                                       ng-show="(compoundEvolverForm.generationSize.$error.number || compoundEvolverForm.generationSize.$error.step || compoundEvolverForm.generationSize.$error.min) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    An integer value (a whole number) above 2 is required
                                </small>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="number-of-generations" class="col-sm-3 col-form-label">Number of
                                generations</label>
                            <div class="col-sm-9">
                                <input type="number"
                                       class="form-control"
                                       ng-model="formModel.numberOfGenerations"
                                       id="number-of-generations"
                                       name="numberOfGenerations"
                                       required="required"
                                       min="2"
                                       step="1"
                                       ng-class="{
                    'is-invalid':!compoundEvolverForm.numberOfGenerations.$valid && (!compoundEvolverForm.numberOfGenerations.$pristine || compoundEvolverForm.$submitted),
                    'is-valid':compoundEvolverForm.numberOfGenerations.$valid && (!compoundEvolverForm.numberOfGenerations.$pristine || compoundEvolverForm.$submitted)}">
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="compoundEvolverForm.numberOfGenerations.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </small>
                                <small class="form-text text-danger"
                                       ng-show="(compoundEvolverForm.numberOfGenerations.$error.number || compoundEvolverForm.numberOfGenerations.$error.step || compoundEvolverForm.numberOfGenerations.$error.min) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    An integer value (a whole number) above 2 is required
                                </small>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="selection-size" class="col-sm-3 col-form-label">Selection size</label>
                            <div class="col-sm-9">
                                <input type="number"
                                       class="form-control"
                                       ng-model="formModel.selectionSize"
                                       id="selection-size"
                                       name="selectionSize"
                                       required="required"
                                       min="0"
                                       max="1"
                                       ng-class="{
                    'is-invalid':!compoundEvolverForm.selectionSize.$valid && (!compoundEvolverForm.selectionSize.$pristine || compoundEvolverForm.$submitted),
                    'is-valid':compoundEvolverForm.selectionSize.$valid && (!compoundEvolverForm.selectionSize.$pristine || compoundEvolverForm.$submitted)}">
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="compoundEvolverForm.selectionSize.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </small>
                                <small class="form-text text-danger"
                                       ng-show="(compoundEvolverForm.selectionSize.$error.number || compoundEvolverForm.selectionSize.$error.min || compoundEvolverForm.selectionSize.$error.max) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A fraction between 0 and 1 is required
                                </small>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="crossover-rate" class="col-sm-3 col-form-label">Crossover rate</label>
                            <div class="col-sm-9">
                                <input type="number"
                                       class="form-control"
                                       ng-model="formModel.crossoverRate"
                                       id="crossover-rate"
                                       name="crossoverRate"
                                       required="required"
                                       min="0"
                                       max="1"
                                       ng-class="{
                    'is-invalid':!compoundEvolverForm.crossoverRate.$valid && (!compoundEvolverForm.crossoverRate.$pristine || compoundEvolverForm.$submitted),
                    'is-valid':compoundEvolverForm.crossoverRate.$valid && (!compoundEvolverForm.crossoverRate.$pristine || compoundEvolverForm.$submitted)}">
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="compoundEvolverForm.crossoverRate.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </small>
                                <small class="form-text text-danger"
                                       ng-show="(compoundEvolverForm.crossoverRate.$error.number || compoundEvolverForm.crossoverRate.$error.min || compoundEvolverForm.crossoverRate.$error.max) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A fraction between 0 and 1 is required
                                </small>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="elitism-rate" class="col-sm-3 col-form-label">Elitism rate</label>
                            <div class="col-sm-9">
                                <input type="number"
                                       class="form-control"
                                       ng-model="formModel.elitismRate"
                                       id="elitism-rate"
                                       name="elitismRate"
                                       required="required"
                                       min="0"
                                       max="1"
                                       ng-class="{
                    'is-invalid':!compoundEvolverForm.elitismRate.$valid && (!compoundEvolverForm.elitismRate.$pristine || compoundEvolverForm.$submitted),
                    'is-valid':compoundEvolverForm.elitismRate.$valid && (!compoundEvolverForm.elitismRate.$pristine || compoundEvolverForm.$submitted)}">
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="compoundEvolverForm.elitismRate.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </small>
                                <small class="form-text text-danger"
                                       ng-show="(compoundEvolverForm.elitismRate.$error.number || compoundEvolverForm.elitismRate.$error.min || compoundEvolverForm.elitismRate.$error.max) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A fraction between 0 and 1 is required
                                </small>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="mutation-rate" class="col-sm-3 col-form-label">Mutation rate</label>
                            <div class="col-sm-9">
                                <input type="number"
                                       class="form-control"
                                       ng-model="formModel.mutationRate"
                                       id="mutation-rate"
                                       name="mutationRate"
                                       required="required"
                                       min="0"
                                       max="1"
                                       ng-class="{
                    'is-invalid':!compoundEvolverForm.mutationRate.$valid && (!compoundEvolverForm.mutationRate.$pristine || compoundEvolverForm.$submitted),
                    'is-valid':compoundEvolverForm.mutationRate.$valid && (!compoundEvolverForm.mutationRate.$pristine || compoundEvolverForm.$submitted)}">
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="compoundEvolverForm.mutationRate.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </small>
                                <small class="form-text text-danger"
                                       ng-show="(compoundEvolverForm.mutationRate.$error.number || compoundEvolverForm.mutationRate.$error.min || compoundEvolverForm.mutationRate.$error.max) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A fraction between 0 and 1 is required
                                </small>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="random-immigrant-rate" class="col-sm-3 col-form-label">Random immigrant
                                rate</label>
                            <div class="col-sm-9">
                                <input type="number"
                                       class="form-control"
                                       ng-model="formModel.randomImmigrantRate"
                                       id="random-immigrant-rate"
                                       name="randomImmigrantRate"
                                       required="required"
                                       min="0"
                                       max="1"
                                       ng-class="{
                    'is-invalid':!compoundEvolverForm.randomImmigrantRate.$valid && (!compoundEvolverForm.randomImmigrantRate.$pristine || compoundEvolverForm.$submitted),
                    'is-valid':compoundEvolverForm.randomImmigrantRate.$valid && (!compoundEvolverForm.randomImmigrantRate.$pristine || compoundEvolverForm.$submitted)}">
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="compoundEvolverForm.randomImmigrantRate.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </small>
                                <small class="form-text text-danger"
                                       ng-show="(compoundEvolverForm.randomImmigrantRate.$error.number || compoundEvolverForm.randomImmigrantRate.$error.min || compoundEvolverForm.randomImmigrantRate.$error.max) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A fraction between 0 and 1 is required
                                </small>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="selection-method" class="col-sm-3 col-form-label">Selection method</label>
                            <div class="col-sm-9">
                                <select class="form-control"
                                        id="selection-method"
                                        name="selectionMethod"
                                        ng-model="formModel.selectionMethod"
                                        required="required">
                                    <option>Fitness proportionate selection</option>
                                    <option>Truncated selection</option>
                                    <option>Tournament selection</option>
                                </select>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="mutation-method" class="col-sm-3 col-form-label">Mutation method</label>
                            <div class="col-sm-9">
                                <select class="form-control"
                                        id="mutation-method"
                                        name="mutationMethod"
                                        ng-model="formModel.mutationMethod"
                                        required="required">
                                    <option>Distance dependent</option>
                                    <option>Distance independent</option>
                                </select>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="termination-condition" class="col-sm-3 col-form-label">Termination
                                condition</label>
                            <div class="col-sm-9">
                                <select class="form-control"
                                        id="termination-condition"
                                        name="terminationCondition"
                                        ng-model="formModel.terminationCondition"
                                        required="required">
                                    <option value="fixed">Maximum number of generations reached</option>
                                    <option value="convergence">Convergence reached</option>
                                </select>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="non-improving-generation-quantity" class="col-sm-3 col-form-label">
                                Non-improving generation amount
                            </label>
                            <div class="col-sm-9">
                                <input type="number"
                                       class="form-control"
                                       ng-model="formModel.nonImprovingGenerationQuantity"
                                       id="non-improving-generation-quantity"
                                       name="nonImprovingGenerationQuantity"
                                       required="required"
                                       min="0"
                                       max="1"
                                       ng-disabled="formModel.terminationCondition == 'fixed'"
                                       ng-class="{
                    'is-invalid':!compoundEvolverForm.nonImprovingGenerationQuantity.$valid && formModel.terminationCondition != 'fixed' && (!compoundEvolverForm.nonImprovingGenerationQuantity.$pristine || compoundEvolverForm.$submitted),
                    'is-valid':compoundEvolverForm.nonImprovingGenerationQuantity.$valid && formModel.terminationCondition != 'fixed' && (!compoundEvolverForm.nonImprovingGenerationQuantity.$pristine || compoundEvolverForm.$submitted)}">
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="compoundEvolverForm.nonImprovingGenerationQuantity.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </small>
                                <small class="form-text text-danger"
                                       ng-show="(compoundEvolverForm.nonImprovingGenerationQuantity.$error.number || compoundEvolverForm.nonImprovingGenerationQuantity.$error.min || compoundEvolverForm.nonImprovingGenerationQuantity.$error.max) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A fraction between 0 and 1 is required
                                </small>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="card">
                    <h5 class="card-header"><b>Scoring</b></h5>
                    <div id="docking-settings" class="card-body">
                        <div class="form-group row">
                            <label for="conformer-count" class="col-sm-3 col-form-label">
                                Maximum number of conformers to generate
                            </label>
                            <div class="col-sm-9">
                                <input type="number"
                                       class="form-control"
                                       ng-model="formModel.conformerCount"
                                       id="conformer-count"
                                       name="conformerCount"
                                       required="required"
                                       min="1"
                                       max="100"
                                       step="1"
                                       ng-class="{
                    'is-invalid':!compoundEvolverForm.conformerCount.$valid && formModel.terminationCondition != 'fixed' && (!compoundEvolverForm.conformerCount.$pristine || compoundEvolverForm.$submitted),
                    'is-valid':compoundEvolverForm.conformerCount.$valid && formModel.terminationCondition != 'fixed' && (!compoundEvolverForm.conformerCount.$pristine || compoundEvolverForm.$submitted)}">
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="compoundEvolverForm.conformerCount.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </small>
                                <small class="form-text text-danger"
                                       ng-show="(compoundEvolverForm.conformerCount.$error.number || compoundEvolverForm.conformerCount.$error.min || compoundEvolverForm.conformerCount.$error.max) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    An integer value (whole number) between 1 and 100 is required
                                </small>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="force-field" class="col-sm-3 col-form-label">Force field</label>
                            <div class="col-sm-9">
                                <select class="form-control"
                                        id="force-field"
                                        ng-model="formModel.forceField"
                                        name="forceField"
                                        required="required">
                                    <option value="smina">smina</option>
                                    <option value="mab">mab(moloc)</option>
                                </select>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="fitness-measure" class="col-sm-3 col-form-label">Fitness measure</label>
                            <div class="col-sm-9">
                                <select class="form-control"
                                        id="fitness-measure"
                                        ng-model="formModel.fitnessMeasure"
                                        name="fitnessMeasure"
                                        required="required">
                                    <option value="ligandEfficiency">Ligand efficiency</option>
                                    <option value="affinity">Affinity</option>
                                </select>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="card">
                    <h5 class="card-header"><b>Filters</b></h5>
                    <div id="filter-settings" class="card-body">
                        <div class="form-group row">
                            <label for="max-anchor-minimized-rmsd" class="col-sm-3 col-form-label">
                                Maximum RMSD allowed from the anchor
                            </label>
                            <div class="col-sm-9">
                                <input type="number"
                                       class="form-control"
                                       ng-model="formModel.maxAnchorMinimizedRmsd"
                                       id="max-anchor-minimized-rmsd"
                                       name="maxAnchorMinimizedRmsd"
                                       min="0"
                                       ng-class="{
                    'is-invalid':!compoundEvolverForm.maxAnchorMinimizedRmsd.$valid && (!compoundEvolverForm.maxAnchorMinimizedRmsd.$pristine || compoundEvolverForm.$submitted),
                    'is-valid':compoundEvolverForm.maxAnchorMinimizedRmsd.$valid && (!compoundEvolverForm.maxAnchorMinimizedRmsd.$pristine || compoundEvolverForm.$submitted)}">
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="compoundEvolverForm.maxAnchorMinimizedRmsd.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </small>
                                <small class="form-text text-danger"
                                       ng-show="(compoundEvolverForm.maxAnchorMinimizedRmsd.$error.number || compoundEvolverForm.maxAnchorMinimizedRmsd.$error.min) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A positive numeric value is required
                                </small>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label class="col-sm-3 col-form-label">Filer application
                            </label>
                            <div class="col-sm-9">
                                <div class="form-check">
                                    <input type="checkbox"
                                           class="form-check-input"
                                           ng-model="formModel.useLipinski"
                                           id="use-lipinski"
                                           name="useLipinski"
                                           value="lipinski">
                                    <label class="form-check-label" for="use-lipinski">
                                        Use Lipinski's / Pfizer's rule of five
                                    </label>
                                </div>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="max-molecular-mass" class="col-sm-3 col-form-label">Maximum molecular mass
                            </label>
                            <div class="col-sm-9">
                                <input type="number"
                                       class="form-control"
                                       ng-model="formModel.maxMolecularMass"
                                       id="max-molecular-mass"
                                       name="maxMolecularMass"
                                       min="0"
                                       ng-disabled="!formModel.useLipinski"
                                       ng-class="{
                    'is-invalid':!compoundEvolverForm.maxMolecularMass.$valid && formModel.useLipinski && (!compoundEvolverForm.maxMolecularMass.$pristine || compoundEvolverForm.$submitted),
                    'is-valid':compoundEvolverForm.maxMolecularMass.$valid && formModel.useLipinski && (!compoundEvolverForm.maxMolecularMass.$pristine || compoundEvolverForm.$submitted)}">
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="(compoundEvolverForm.maxMolecularMass.$error.number || compoundEvolverForm.maxMolecularMass.$error.min) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A positive numeric value is required
                                </small>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="max-hydrogen-bond-donors" class="col-sm-3 col-form-label">Maximum hydrogen bond
                                donors
                            </label>
                            <div class="col-sm-9">
                                <input type="number"
                                       class="form-control"
                                       ng-model="formModel.maxHydrogenBondDonors"
                                       id="max-hydrogen-bond-donors"
                                       name="maxHydrogenBondDonors"
                                       min="0"
                                       ng-disabled="!formModel.useLipinski"
                                       ng-class="{
                    'is-invalid':!compoundEvolverForm.maxHydrogenBondDonors.$valid && formModel.useLipinski && (!compoundEvolverForm.maxHydrogenBondDonors.$pristine || compoundEvolverForm.$submitted),
                    'is-valid':compoundEvolverForm.maxHydrogenBondDonors.$valid && formModel.useLipinski && (!compoundEvolverForm.maxHydrogenBondDonors.$pristine || compoundEvolverForm.$submitted)}">
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="(compoundEvolverForm.maxHydrogenBondDonors.$error.number || compoundEvolverForm.maxHydrogenBondDonors.$error.min) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A positive numeric value is required
                                </small>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="max-hydrogen-bond-acceptors" class="col-sm-3 col-form-label">Maximum hydrogen
                                bond acceptors
                            </label>
                            <div class="col-sm-9">
                                <input type="number"
                                       class="form-control"
                                       ng-model="formModel.maxHydrogenBondAcceptors"
                                       id="max-hydrogen-bond-acceptors"
                                       name="maxHydrogenBondAcceptors"
                                       min="0"
                                       ng-disabled="!formModel.useLipinski"
                                       ng-class="{
                    'is-invalid':!compoundEvolverForm.maxHydrogenBondAcceptors.$valid && formModel.useLipinski && (!compoundEvolverForm.maxHydrogenBondAcceptors.$pristine || compoundEvolverForm.$submitted),
                    'is-valid':compoundEvolverForm.maxHydrogenBondAcceptors.$valid && formModel.useLipinski && (!compoundEvolverForm.maxHydrogenBondAcceptors.$pristine || compoundEvolverForm.$submitted)}">
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="(compoundEvolverForm.maxHydrogenBondAcceptors.$error.number || compoundEvolverForm.maxHydrogenBondAcceptors.$error.min) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A positive numeric value is required
                                </small>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="max-partition-coefficient" class="col-sm-3 col-form-label">Maximum octanol-water
                                partition coefficient logP value
                            </label>
                            <div class="col-sm-9">
                                <input type="number"
                                       class="form-control"
                                       ng-model="formModel.maxPartitionCoefficient"
                                       id="max-partition-coefficient"
                                       name="maxPartitionCoefficient"
                                       min="0"
                                       ng-disabled="!formModel.useLipinski"
                                       ng-class="{
                    'is-invalid':!compoundEvolverForm.maxPartitionCoefficient.$valid && formModel.useLipinski && (!compoundEvolverForm.maxPartitionCoefficient.$pristine || compoundEvolverForm.$submitted),
                    'is-valid':compoundEvolverForm.maxPartitionCoefficient.$valid && formModel.useLipinski && (!compoundEvolverForm.maxPartitionCoefficient.$pristine || compoundEvolverForm.$submitted)}">
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="(compoundEvolverForm.maxPartitionCoefficient.$error.number || compoundEvolverForm.maxPartitionCoefficient.$error.min) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A positive numeric value is required
                                </small>
                            </div>
                        </div>
                    </div>
                </div>
                <div id="submit">
                    <div class="card card-body">
                        <div class="form-group row">
                            <div class="col-sm-9 offset-sm-3">
                                <button type="submit" class="btn btn-primary"
                                        ng-click="onSubmit(
                                (!reactionFiles.wrongExtension && reactionFiles.hasFile) &&
                                (!reactantFiles.wrongExtension && reactantFiles.hasFile) &&
                                (!receptorFile.wrongExtension && receptorFile.hasFile) &&
                                (!anchorFragmentFile.wrongExtension && anchorFragmentFile.hasFile) &&
                                compoundEvolverForm.$valid)">
                                    Submit
                                </button>
                                <button type="button" class="btn btn-danger"
                                        ng-click="terminateEvolution()">
                                    Terminate
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </form>
        </div>
    </div>
    <div class="row">
        <div class="col-lg-12">
            <h5>Results</h5>
            <ul class="nav nav-pills">
                <li class="nav-item">
                    <a class="nav-link disabled" href>Download selected generation</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href ng-click="downloadRun()">Download all</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href ng-click="downloadCsv()">Download csv</a>
                </li>
            </ul>
            <canvas id="score-distribution-chart" width="400" height="400"></canvas>
            <canvas id="species-distribution-chart" width="400" height="400"></canvas>
            <h6>Selected generation</h6>
            <table class="table table-condensed table-borderless mono-font">
                <thead>
                <tr>
                    <th>ID</th>
                    <th>COMPOUND</th>
                    <th class="text-muted">SCORE</th>
                    <th><abbr title="Ligand Efficiency">LE</abbr></th>
                    <th><abbr title="Ligand Lipophilicity Efficiency">LEE</abbr></th>
                </tr>
                </thead>
                <tbody>
                <tr ng-repeat="candidate in getPopulation() | orderBy:'ligandEfficiency'">
                    <td><a href ng-click="downloadCompound(candidate.id)">{{candidate.id}}</a></td>
                    <td>{{candidate.smiles}}</td>
                    <td>{{candidate.rawScore | number:4}}</td>
                    <td>{{candidate.ligandEfficiency | number:4}}</td>
                    <td>{{candidate.ligandLipophilicityEfficiency | number:4}}</td>
                </tr>
                </tbody>
            </table>
        </div>
    </div>
</div>
</body>
</html>
