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
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css"
          integrity="sha384-Gn5384xqQ1aoWXA+058RXPxPg6fy4IWvTNh0E263XmFcJlSAwiGgFAW/dAiS6JXm"
          crossorigin="anonymous">
    <link rel="stylesheet" href="<c:url value = "css/main.css"/>">
    <link rel="stylesheet" href="<c:url value = "css/dashboard.css"/>">
    <%--load library javascript--%>
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script>
    <script
            src="https://code.jquery.com/ui/1.12.1/jquery-ui.min.js"
            integrity="sha256-VazP97ZCwtekAsvgPBSUwPFKdrwD3unUfSGVYrahUqU="
            crossorigin="anonymous"></script>
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"
            integrity="sha384-JZR6Spejh4U02d8jOt6vLEHfe/JQGiRRSQQxSfFWpi1MquVdAyjUar5+76PVCmYl"
            crossorigin="anonymous"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.6.7/angular.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.7.2/Chart.min.js"></script>
    <script src="https://unpkg.com/chartjs-chart-box-and-violin-plot"></script>
    <%--load custom javascript--%>
    <script src="<c:url value = "js/app.js"/>"></script>
    <script src="<c:url value = "js/modules/filereadbinding.js"/>"></script>
</head>
<body>
<nav class="navbar navbar-dark sticky-top bg-dark flex-md-nowrap p-0">
    <a class="navbar-brand col-sm-4 col-md-3 mr-0" href="#">Compound evolver</a>
    <input class="form-control form-control-dark w-100" type="text" placeholder="Search" aria-label="Search">
    <ul class="navbar-nav px-3">
        <li class="nav-item text-nowrap">
            <a class="nav-link" href="#">Sign out</a>
        </li>
    </ul>
</nav>

<div class="container-fluid" ng-controller="FormInputCtrl">
    <div class="row">
        <nav class="col-md-3 d-none d-md-block bg-light sidebar">
            <div class="sidebar-sticky">
                <div class="nav flex-column">
                <form name="compoundEvolverForm"
                      novalidate="novalidate"
                      enctype="multipart/form-data">
                    <div class="alert alert-danger" ng-show="response.hasError" ng-bind="response.error"></div>
                    <h5><b>Building blocks and reaction</b></h5>
                    <div id="file-input">
                        <div class="form-group row">
                            <label for="reaction-file" class="col-sm-3 col-form-label">Reaction file (.mrv)</label>
                            <div class="col-sm-9">
                                <label for="reaction-file" class="custom-file-upload">
                                    <strong class="btn btn-secondary">Choose file</strong>
                                    <span ng-bind="formModel.reactionFile[0].files[0].name"
                                          ng-class="{
                    'text-danger':reactionFile.wrongExtension || (!reactionFile.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)),
                    'text-success':!reactionFile.wrongExtension && (!reactionFile.pristine || compoundEvolverForm.$submitted)}"></span>
                                </label>
                                <input type="file"
                                       file-bind="formModel.reactionFile"
                                       id="reaction-file"
                                       name="reactionFile"
                                       required="required"/>
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="!reactionFile.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </small>
                                <small class="form-text text-danger"
                                       ng-show="reactionFile.wrongExtension">
                                    Only an mrv file (.mrv) is accepted
                                </small>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="reactant-files" class="col-sm-3 float-sm-left col-form-label">Reactants files
                                (.smiles,
                                .smi)</label>
                            <div class="col-sm-9 float-sm-left">
                                <label for="reactant-files" class="custom-file-upload">
                                    <strong class="btn btn-secondary">Choose files</strong>
                                </label>
                                <input type="file"
                                       file-bind="formModel.reactantFiles"
                                       id="reactant-files"
                                       name="reactantFiles"
                                       required="required"
                                       multiple="multiple"/>
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
                        <div class="form-group row">
                            <label class="col-sm-3 col-form-label">Order of reactant files in reaction</label>
                            <div class="col-sm-9">
                                <ul class="list-group" id="sortableReactantList">
                                    <li class="list-group-item" id="file-list-item-{{$index}}"
                                        ng-repeat="file in reactantFiles.files track by $index">
                                    <span class=""
                                          ng-class="{
                                    'text-danger':file.invalid}">{{file.name}}</span>
                                    </li>
                                </ul>
                                <ul class="list-group" ng-hide="reactantFiles.files.length">
                                    <li class="list-group-item disabled">Please choose reactant files</li>
                                </ul>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="receptor-file" class="col-sm-3 col-form-label">Receptor file (.pdb)</label>
                            <div class="col-sm-9">
                                <label for="receptor-file" class="custom-file-upload">
                                    <strong class="btn btn-secondary">Choose file</strong>
                                    <span ng-bind="formModel.receptorFile[0].files[0].name"
                                          ng-class="{
                    'text-danger':receptorFile.wrongExtension || (!receptorFile.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)),
                    'text-success':!receptorFile.wrongExtension && (!receptorFile.pristine || compoundEvolverForm.$submitted)}"></span>
                                </label>
                                <input type="file"
                                       file-bind="formModel.receptorFile"
                                       id="receptor-file"
                                       name="receptorFile"
                                       required="required"/>
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
                                <label for="anchor-fragment-file" class="custom-file-upload">
                                    <strong class="btn btn-secondary">Choose file</strong>
                                    <span ng-bind="formModel.anchorFragmentFile[0].files[0].name"
                                          ng-class="{
                    'text-danger':anchorFragmentFile.wrongExtension || (!anchorFragmentFile.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)),
                    'text-success':!anchorFragmentFile.wrongExtension && (!anchorFragmentFile.pristine || compoundEvolverForm.$submitted)}"></span>
                                </label>
                                <input type="file"
                                       file-bind="formModel.anchorFragmentFile"
                                       id="anchor-fragment-file"
                                       name="anchorFragmentFile"
                                       required="required"/>
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
                    <h5><b>Genetic operators</b></h5>
                    <div id="operator-settings">
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
                    <h5><b>Scoring</b></h5>
                    <div id="docking-settings">
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
                    <h5><b>Filters</b></h5>
                    <div id="filter-settings">
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
                    <div id="submit">
                        <div class="form-group row">
                            <div class="col-sm-9 offset-sm-3">
                                <button type="submit" class="btn btn-primary"
                                        ng-click="onSubmit(
                                (!reactionFile.wrongExtension && reactionFile.hasFile) &&
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
                </form>
            </div>
        </nav>
    </div>
        <main role="main" class="col-md-8 ml-sm-auto col-lg-9 pt-3 px-4">
            <div style="position: absolute; left: 0px; top: 0px; right: 0px; bottom: 0px; overflow: hidden; pointer-events: none; visibility: hidden; z-index: -1;"
                 class="chartjs-size-monitor">
                <div class="chartjs-size-monitor-expand"
                     style="position:absolute;left:0;top:0;right:0;bottom:0;overflow:hidden;pointer-events:none;visibility:hidden;z-index:-1;">
                    <div style="position:absolute;width:1000000px;height:1000000px;left:0;top:0"></div>
                </div>
                <div class="chartjs-size-monitor-shrink"
                     style="position:absolute;left:0;top:0;right:0;bottom:0;overflow:hidden;pointer-events:none;visibility:hidden;z-index:-1;">
                    <div style="position:absolute;width:200%;height:200%;left:0; top:0"></div>
                </div>
            </div>
            <div class="d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pb-2 mb-3 border-bottom">
                <h1 class="h2">Results</h1>
                <div class="btn-toolbar mb-2 mb-md-0">
                    <div class="btn-group mr-2">
                        <button class="btn btn-sm btn-outline-secondary">Share</button>
                        <button class="btn btn-sm btn-outline-secondary">Export</button>
                    </div>
                    <button class="btn btn-sm btn-outline-secondary dropdown-toggle">
                        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none"
                             stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
                             class="feather feather-calendar">
                            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                            <line x1="16" y1="2" x2="16" y2="6"></line>
                            <line x1="8" y1="2" x2="8" y2="6"></line>
                            <line x1="3" y1="10" x2="21" y2="10"></line>
                        </svg>
                        This week
                    </button>
                </div>
            </div>

            <canvas class="my-4 chartjs-render-monitor" id="myChart" width="1538" height="649"
                    style="display: block; width: 1538px; height: 649px;"></canvas>

            <h2>Section title</h2>
            <div class="table-responsive">
                <table class="table table-sm table-condensed table-borderless mono-font">
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
        </main>
    </div>
</div>
</body>
</html>
