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
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"/>
    <%--load library javascript--%>
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script>
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>
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
            <form class="form-horizontal"
                  name="compoundEvolverForm"
                  novalidate="novalidate"
                  enctype="multipart/form-data">
                <div class="alert alert-danger" ng-show="response.hasError" ng-bind="response.error">
                </div>
                <div class="form-group" ng-class="{
                    'has-error':file.wrongExtension || (!file.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)),
                    'has-success':!file.wrongExtension && !file.pristine}">
                    <label for="file" class="control-label col-sm-3">Upload file:</label>
                    <div class="col-sm-9">
                        <label for="file" class="custom-file-upload">
                            <strong class="btn btn-default">Choose file</strong>
                            <span ng-bind="formModel.file[0].files[0].name"></span>
                        </label>
                        <input type="file"
                               file-bind="formModel.file"
                               id="file"
                               name="file"
                               required="required"/>
                    </div>
                    <p class="help-block col-sm-9 pull-right"
                       ng-show="!file.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                        This field is required
                    </p>
                    <p class="help-block col-sm-9 pull-right"
                       ng-show="file.wrongExtension">
                        Only an mrv file (.mrv) is accepted
                    </p>
                </div>
                <div class="form-group" ng-class="{
                    'has-error':!compoundEvolverForm.generationSize.$valid && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted),
                    'has-success':compoundEvolverForm.generationSize.$valid && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)}">
                    <label for="generation-size" class="control-label col-sm-3">Generation size:</label>
                    <div class="col-sm-9">
                        <input type="number"
                               class="form-control"
                               ng-model="formModel.generationSize"
                               id="generation-size"
                               name="generationSize"
                               required="required"
                               min="2"
                               step="1">
                    </div>
                    <p class="help-block col-sm-9 pull-right"
                       ng-show="compoundEvolverForm.generationSize.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                        This field is required
                    </p>
                    <p class="help-block col-sm-9 pull-right"
                       ng-show="(compoundEvolverForm.generationSize.$error.number || compoundEvolverForm.generationSize.$error.step || compoundEvolverForm.generationSize.$error.min) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                        An integer value (a whole number) above 2 is required
                    </p>
                </div>
                <div class="form-group" ng-class="{
                    'has-error':!compoundEvolverForm.mutationRate.$valid && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted),
                    'has-success':compoundEvolverForm.mutationRate.$valid && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)}">
                    <label for="mutation-rate" class="control-label col-sm-3">Mutation rate:</label>
                    <div class="col-sm-9">
                        <input type="number"
                               class="form-control"
                               ng-model="formModel.mutationRate"
                               id="mutation-rate"
                               name="mutationRate"
                               required="required"
                               min="0"
                               max="1">
                    </div>
                    <p class="help-block col-sm-9 pull-right"
                       ng-show="compoundEvolverForm.mutationRate.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                        This field is required
                    </p>
                    <p class="help-block col-sm-9 pull-right"
                       ng-show="(compoundEvolverForm.mutationRate.$error.number || compoundEvolverForm.mutationRate.$error.min || compoundEvolverForm.mutationRate.$error.max) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                        A fraction between 0 and 1 is required
                    </p>
                </div>
                <div class="form-group" ng-class="{
                    'has-error':!compoundEvolverForm.crossoverRate.$valid && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted),
                    'has-success':compoundEvolverForm.crossoverRate.$valid && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)}">
                    <label for="crossover-rate" class="control-label col-sm-3">Crossover rate:</label>
                    <div class="col-sm-9">
                        <input type="number"
                               class="form-control"
                               ng-model="formModel.crossoverRate"
                               id="crossover-rate"
                               name="crossoverRate"
                               required="required"
                               min="0"
                               max="1">
                    </div>
                    <p class="help-block col-sm-9 pull-right"
                       ng-show="compoundEvolverForm.crossoverRate.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                        This field is required
                    </p>
                    <p class="help-block col-sm-9 pull-right"
                       ng-show="(compoundEvolverForm.crossoverRate.$error.number || compoundEvolverForm.crossoverRate.$error.min || compoundEvolverForm.crossoverRate.$error.max) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                        A fraction between 0 and 1 is required
                    </p>
                </div>
                <br/>
                <button type="submit" class="btn btn-default"
                        ng-click="onSubmit((!file.wrongExtension) && (file.hasFile) && compoundEvolverForm.$valid)">Submit
                </button>
            </form>
        </div>
    </div>
</div>
</body>
</html>
