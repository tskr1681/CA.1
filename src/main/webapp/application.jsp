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
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js"
            integrity="sha384-JZR6Spejh4U02d8jOt6vLEHfe/JQGiRRSQQxSfFWpi1MquVdAyjUar5+76PVCmYl"
            crossorigin="anonymous"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.6.7/angular.min.js"></script>
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
            <form
                    name="compoundEvolverForm"
                    novalidate="novalidate"
                    enctype="multipart/form-data">
                <div class="alert alert-danger" ng-show="response.hasError" ng-bind="response.error">
                </div>
                <div class="form-group">
                    <div class="row"><label for="file" class="col-sm-3 col-form-label">Upload mrv reaction file:</label>
                        <div class="col-sm-9">
                            <label for="file" class="custom-file-upload"
                                   ng-class="{
                    'is-invalid':file.wrongExtension || (!file.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)),
                    'is-valid':!file.wrongExtension && !file.pristine}">
                                <strong class="btn btn-secondary">Choose file</strong>
                                <span ng-bind="formModel.file[0].files[0].name"></span>
                            </label>
                            <input type="file"
                                   file-bind="formModel.file"
                                   id="file"
                                   name="file"
                                   required="required"/>
                        </div>
                        <div class="col"><p class="form-text text-danger col-sm-9 float-sm-right"
                           ng-show="!file.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                            This field is required
                        </p>
                        <p class="form-text text-danger col-sm-9 float-sm-right"
                           ng-show="file.wrongExtension">
                            Only an mrv file (.mrv) is accepted
                        </p></div>
                    </div>
                </div>
                <button type="button" class="btn btn-link" data-toggle="collapse" data-target="#operator-settings">
                    Genetic operator settings
                </button>
                <div id="operator-settings" class="collapse">
                    <div class="form-group">
                        <div class="row"><label for="generation-size" class="col-sm-3 col-form-label">Generation
                            size:</label>
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
                            <div class="col"><p class="form-text text-danger col-sm-9 float-sm-right"
                               ng-show="compoundEvolverForm.generationSize.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                This field is required
                            </p>
                            <p class="form-text text-danger col-sm-9 float-sm-right"
                               ng-show="(compoundEvolverForm.generationSize.$error.number || compoundEvolverForm.generationSize.$error.step || compoundEvolverForm.generationSize.$error.min) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                An integer value (a whole number) above 2 is required
                            </p></div>
                        </div>
                    </div>
                    <div class="form-group">
                        <div class="row"><label for="mutation-rate" class="col-sm-3 col-form-label">Mutation
                            rate:</label>
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
                            <div class="col"><p class="form-text text-danger col-sm-9 float-sm-right"
                               ng-show="compoundEvolverForm.mutationRate.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                This field is required
                            </p>
                            <p class="form-text text-danger col-sm-9 float-sm-right"
                               ng-show="(compoundEvolverForm.mutationRate.$error.number || compoundEvolverForm.mutationRate.$error.min || compoundEvolverForm.mutationRate.$error.max) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                A fraction between 0 and 1 is required
                            </p></div>
                        </div>
                    </div>
                    <div class="form-group">
                        <div class="row"><label for="crossover-rate" class="col-sm-3 col-form-label">Crossover
                            rate:</label>
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
                            <div class="col"><p class="form-text text-danger col-sm-9 float-sm-right"
                               ng-show="compoundEvolverForm.crossoverRate.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                This field is required
                            </p>
                            <p class="form-text text-danger col-sm-9 float-sm-right"
                               ng-show="(compoundEvolverForm.crossoverRate.$error.number || compoundEvolverForm.crossoverRate.$error.min || compoundEvolverForm.crossoverRate.$error.max) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                A fraction between 0 and 1 is required
                            </p></div>
                        </div>
                    </div>
                    <div class="form-group">
                        <div class="row"><label for="random-immigrant-portion" class="col-sm-3 col-form-label">Random
                            immigrant
                            portion:</label>
                            <div class="col-sm-9">
                                <input type="number"
                                       class="form-control"
                                       ng-model="formModel.randomImmigrantPortion"
                                       id="random-immigrant-portion"
                                       name="randomImmigrantPortion"
                                       required="required"
                                       min="0"
                                       max="1"
                                       ng-class="{
                    'is-invalid':!compoundEvolverForm.randomImmigrantPortion.$valid && (!compoundEvolverForm.randomImmigrantPortion.$pristine || compoundEvolverForm.$submitted),
                    'is-valid':compoundEvolverForm.randomImmigrantPortion.$valid && (!compoundEvolverForm.randomImmigrantPortion.$pristine || compoundEvolverForm.$submitted)}">
                            </div>
                            <div class="col"><p class="form-text text-danger col-sm-9 float-sm-right"
                                 ng-show="compoundEvolverForm.randomImmigrantPortion.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                This field is required
                            </p>
                            <p class="form-text text-danger col-sm-9 float-sm-right"
                                 ng-show="(compoundEvolverForm.randomImmigrantPortion.$error.number || compoundEvolverForm.randomImmigrantPortion.$error.min || compoundEvolverForm.randomImmigrantPortion.$error.max) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                A fraction between 0 and 1 is required
                            </p></div>
                        </div>
                    </div>
                    <div class="form-group">
                        <div class="row"><label for="selection-method" class="col-sm-3 col-form-label">Selection
                            method:</label>
                            <div class="col-sm-9">
                                <select class="form-control"
                                        id="selection-method"
                                        ng-model="formModel.selectionMethod"
                                        required="required">
                                    <option>Fitness proportionate selection</option>
                                    <option>Truncated selection</option>
                                </select>
                            </div>
                        </div>
                    </div>
                    <div class="form-group">
                        <div class="row"><label for="mutation-method" class="col-sm-3 col-form-label">Mutation
                            method:</label>
                            <div class="col-sm-9">
                                <select class="form-control" id="mutation-method" ng-model="formModel.mutationMethod">
                                    <option>Distance dependent</option>
                                    <option>Distance independent</option>
                                </select>
                            </div>
                        </div>
                    </div>
                </div>
                <br/>
                <button type="submit" class="btn btn-default"
                        ng-click="onSubmit((!file.wrongExtension) && (file.hasFile) && compoundEvolverForm.$valid)">
                    Submit
                </button>
            </form>
        </div>
    </div>
</div>
</body>
</html>
