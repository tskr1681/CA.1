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
    <%--load custom javascript--%>
    <script src="<c:url value = "js/app.js"/>"></script>
    <script src="<c:url value = "js/modules/filereadbinding.js"/>"></script>
</head>
<body>
<div class="container">
    <div class="page-header">
        <h1>Compound Evolver Application</h1>
    </div>
    <div class="row">
        <div class="col-lg-12">
            <h2>Introduction</h2>
            <p class="lead">
                Lead
            </p>
            <p>
                Paragraph
            </p>
        </div>
    </div>
    <%--Setup dynamic form using angularjs--%>
    <div class="row">
        <div class="col-lg-12" ng-controller="FormInputCtrl">
            <h2>Set genetic algorithm parameters</h2>
            <form name="compoundEvolverForm"
                  novalidate="novalidate"
                  enctype="multipart/form-data">
                <div class="alert alert-danger" ng-show="response.hasError" ng-bind="response.error"></div>
                <div class="card card-body">
                    <h5 class="card-title"><b>Building blocks and reaction</b></h5>
                    <div class="form-group row">
                        <label for="reaction-file" class="col-sm-3 col-form-label">Upload reaction file (.mrv):</label>
                        <div class="col-sm-9">
                            <label for="reaction-file" class="custom-file-upload">
                                <strong class="btn btn-secondary">Choose file</strong>
                                <span ng-bind="formModel.reactionFile[0].files[0].name"
                                      ng-class="{
                    'text-danger':reactionFile.wrongExtension || (!reactionFile.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)),
                    'text-success':!reactionFile.wrongExtension && !reactionFile.pristine}"></span>
                            </label>
                            <input type="file"
                                   file-bind="formModel.reactionFile"
                                   id="reaction-file"
                                   name="reactionFile"
                                   required="required"/>
                        </div>
                        <div class="col-sm-9 offset-sm-3">
                            <p class="form-text text-danger"
                               ng-show="!reactionFile.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                This field is required
                            </p>
                            <p class="form-text text-danger"
                               ng-show="reactionFile.wrongExtension">
                                Only an mrv file (.mrv) is accepted
                            </p></div>
                    </div>
                    <div class="form-group row">
                        <label for="reactant-files" class="col-sm-3 col-form-label">Upload reactants files (.smiles,
                            .smi):</label>
                        <div class="col-sm-9">
                            <label for="reactant-files" class="custom-file-upload">
                                <strong class="btn btn-secondary">Choose files</strong>
                                <%--<span ng-bind="reactantFiles.names"--%>
                                <%--ng-class="{--%>
                                <%--'text-danger':reactantFiles.wrongExtension || (!reactantFiles.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)),--%>
                                <%--'text-success':!reactantFiles.wrongExtension && !reactantFiles.pristine}"></span>--%>
                            </label>
                            <input type="file"
                                   file-bind="formModel.reactantFiles"
                                   id="reactant-files"
                                   name="reactantFiles"
                                   required="required"
                                   multiple="multiple"/>
                        </div>
                        <div class="col-sm-9 offset-sm-3">
                            <p class="form-text text-danger"
                               ng-show="!reactantFiles.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                This field is required
                            </p>
                            <p class="form-text text-danger"
                               ng-show="reactantFiles.wrongExtension">
                                Only smiles files (.smiles, .smi) are accepted
                            </p></div>
                    </div>
                    <div class="row">
                        <div class="col-sm-9 offset-sm-3">
                            <ul class="list-unstyled" id="sortable">
                                <li id="file-list-item-{{$index}}" ng-repeat="file in reactantFiles.files track by $index">
                                    <span class="badge badge-secondary"
                                    ng-class="{
                                    'badge-danger':file.invalid,
                                    'badge-success':!file.invalid }">{{file.name}}</span>
                                </li>
                            </ul>
                        </div>
                    </div>
                </div>
                <%--<button type="button" class="btn btn-link" data-toggle="collapse" data-target="#operator-settings">--%>
                <%--Genetic operator settings--%>
                <%--</button>--%>
                <div id="operator-settings">
                    <div class="card card-body">
                        <h5 class="card-title"><b>Genetic operators</b></h5>
                        <div class="form-group row">
                            <label for="generation-size" class="col-sm-3 col-form-label">Size of initial
                                generation:</label>
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
                                <p class="form-text text-danger"
                                   ng-show="compoundEvolverForm.generationSize.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </p>
                                <p class="form-text text-danger"
                                   ng-show="(compoundEvolverForm.generationSize.$error.number || compoundEvolverForm.generationSize.$error.step || compoundEvolverForm.generationSize.$error.min) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    An integer value (a whole number) above 2 is required
                                </p>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="number-of-generations" class="col-sm-3 col-form-label">Number of
                                generations:</label>
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
                                <p class="form-text text-danger"
                                   ng-show="compoundEvolverForm.numberOfGenerations.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </p>
                                <p class="form-text text-danger"
                                   ng-show="(compoundEvolverForm.numberOfGenerations.$error.number || compoundEvolverForm.numberOfGenerations.$error.step || compoundEvolverForm.numberOfGenerations.$error.min) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    An integer value (a whole number) above 2 is required
                                </p>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="selection-size" class="col-sm-3 col-form-label">Selection size:</label>
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
                                <p class="form-text text-danger"
                                   ng-show="compoundEvolverForm.selectionSize.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </p>
                                <p class="form-text text-danger"
                                   ng-show="(compoundEvolverForm.selectionSize.$error.number || compoundEvolverForm.selectionSize.$error.min || compoundEvolverForm.selectionSize.$error.max) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A fraction between 0 and 1 is required
                                </p>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="crossover-rate" class="col-sm-3 col-form-label">Crossover rate:</label>
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
                                <p class="form-text text-danger"
                                   ng-show="compoundEvolverForm.crossoverRate.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </p>
                                <p class="form-text text-danger"
                                   ng-show="(compoundEvolverForm.crossoverRate.$error.number || compoundEvolverForm.crossoverRate.$error.min || compoundEvolverForm.crossoverRate.$error.max) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A fraction between 0 and 1 is required
                                </p>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="elitist-rate" class="col-sm-3 col-form-label">Elitist rate:</label>
                            <div class="col-sm-9">
                                <input type="number"
                                       class="form-control"
                                       ng-model="formModel.elitistRate"
                                       id="elitist-rate"
                                       name="elitistRate"
                                       required="required"
                                       min="0"
                                       max="1"
                                       ng-class="{
                    'is-invalid':!compoundEvolverForm.elitistRate.$valid && (!compoundEvolverForm.elitistRate.$pristine || compoundEvolverForm.$submitted),
                    'is-valid':compoundEvolverForm.elitistRate.$valid && (!compoundEvolverForm.elitistRate.$pristine || compoundEvolverForm.$submitted)}">
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <p class="form-text text-danger"
                                   ng-show="compoundEvolverForm.elitistRate.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </p>
                                <p class="form-text text-danger"
                                   ng-show="(compoundEvolverForm.elitistRate.$error.number || compoundEvolverForm.elitistRate.$error.min || compoundEvolverForm.elitistRate.$error.max) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A fraction between 0 and 1 is required
                                </p>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="mutation-rate" class="col-sm-3 col-form-label">Mutation rate:</label>
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
                                <p class="form-text text-danger"
                                   ng-show="compoundEvolverForm.mutationRate.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </p>
                                <p class="form-text text-danger"
                                   ng-show="(compoundEvolverForm.mutationRate.$error.number || compoundEvolverForm.mutationRate.$error.min || compoundEvolverForm.mutationRate.$error.max) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A fraction between 0 and 1 is required
                                </p>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="random-immigrant-rate" class="col-sm-3 col-form-label">Random immigrant
                                rate:</label>
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
                                <p class="form-text text-danger"
                                   ng-show="compoundEvolverForm.randomImmigrantRate.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </p>
                                <p class="form-text text-danger"
                                   ng-show="(compoundEvolverForm.randomImmigrantRate.$error.number || compoundEvolverForm.randomImmigrantRate.$error.min || compoundEvolverForm.randomImmigrantRate.$error.max) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A fraction between 0 and 1 is required
                                </p>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="selection-method" class="col-sm-3 col-form-label">Selection method:</label>
                            <div class="col-sm-9">
                                <select class="form-control"
                                        id="selection-method"
                                        name="selectionMethod"
                                        ng-model="formModel.selectionMethod"
                                        required="required">
                                    <option>Fitness proportionate selection</option>
                                    <option>Truncated selection</option>
                                </select>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="mutation-method" class="col-sm-3 col-form-label">Mutation method:</label>
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
                                condition:</label>
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
                                Non-improving generation amount:
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
                                <p class="form-text text-danger"
                                   ng-show="compoundEvolverForm.nonImprovingGenerationQuantity.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </p>
                                <p class="form-text text-danger"
                                   ng-show="(compoundEvolverForm.nonImprovingGenerationQuantity.$error.number || compoundEvolverForm.nonImprovingGenerationQuantity.$error.min || compoundEvolverForm.nonImprovingGenerationQuantity.$error.max) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A fraction between 0 and 1 is required
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
                <%--<button type="button" class="btn btn-link" data-toggle="collapse" data-target="#docking-settings">--%>
                <%--Docking settings--%>
                <%--</button>--%>
                <div id="docking-settings">
                    <div class="card card-body">
                        <h5 class="card-title"><b>Docking</b></h5>
                        <div class="form-group row">
                            <label for="force-field" class="col-sm-3 col-form-label">Force field:</label>
                            <div class="col-sm-9">
                                <select class="form-control"
                                        id="force-field"
                                        ng-model="formModel.forceField"
                                        required="required">
                                    <option>mmff94</option>
                                    <option>mab(moloc)</option>
                                </select>
                            </div>
                        </div>
                    </div>
                </div>
                <div id="filter-settings">
                    <div class="card card-body">
                        <h5 class="card-title"><b>Filters</b></h5>
                        <div class="form-group row">
                            <div class="form-check col-sm-9 offset-sm-3">
                                <input type="checkbox"
                                       class="form-check-input"
                                       ng-model="formModel.useLipinski"
                                       id="use-lipinski"
                                       name="useLipinski"
                                       value="lipinski">
                                <label class="form-check-label"
                                       for="use-lipinski">Use Lipinski's / Pfizer's rule of five</label>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="max-molecular-mass" class="col-sm-3 col-form-label">Maximum molecular mass:
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
                                <p class="form-text text-danger"
                                   ng-show="(compoundEvolverForm.maxMolecularMass.$error.number || compoundEvolverForm.maxMolecularMass.$error.min) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A positive numeric value is required
                                </p>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="max-hydrogen-bond-donors" class="col-sm-3 col-form-label">Maximum hydrogen bond
                                donors:
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
                                <p class="form-text text-danger"
                                   ng-show="(compoundEvolverForm.maxHydrogenBondDonors.$error.number || compoundEvolverForm.maxHydrogenBondDonors.$error.min) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A positive numeric value is required
                                </p>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="max-hydrogen-bond-acceptors" class="col-sm-3 col-form-label">Maximum hydrogen
                                bond acceptors:
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
                                <p class="form-text text-danger"
                                   ng-show="(compoundEvolverForm.maxHydrogenBondAcceptors.$error.number || compoundEvolverForm.maxHydrogenBondAcceptors.$error.min) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A positive numeric value is required
                                </p>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="max-partition-coefficient" class="col-sm-3 col-form-label">Maximum octanol-water
                                partition coefficient logP value:
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
                                <p class="form-text text-danger"
                                   ng-show="(compoundEvolverForm.maxPartitionCoefficient.$error.number || compoundEvolverForm.maxPartitionCoefficient.$error.min) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A positive numeric value is required
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="form-group row">
                    <div class="col-sm-9 offset-sm-3">
                        <button type="submit" class="btn btn-default"
                                ng-click="onSubmit((!reactionFile.wrongExtension) && (reactionFile.hasFile) && compoundEvolverForm.$valid)">
                            Submit
                        </button>
                    </div>
                </div>
            </form>
            <canvas id="myChart" width="400" height="400"></canvas>
        </div>
    </div>
</div>
</body>
</html>
