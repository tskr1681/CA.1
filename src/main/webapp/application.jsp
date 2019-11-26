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
    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.6.7/angular.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.7.2/Chart.min.js"></script>
    <script src="https://unpkg.com/chartjs-chart-box-and-violin-plot@1.2.1"></script>
    <script src="<c:url value = "js/libs/angularjs-dropdown-multiselect.js"/>"></script>
    <%--load custom javascript--%>
    <script src="<c:url value = "js/app.js"/>"></script>
    <script src="<c:url value = "js/modules/filereadbinding.js"/>"></script>
    <script src="https://unpkg.com/ngl@0.10.4/dist/ngl.js"></script>
    <script src="https://unpkg.com/smiles-drawer@1.0.2/dist/smiles-drawer.min.js"></script>
    <script src="marvin/gui/lib/promise-1.0.0.min.js"></script>
    <script src="marvin/js/marvinjslauncher.js"></script>
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
            <p class="lead">
                This web application presents a new genetic algorithm (GA) that aims to find the best 'druglike'
                compounds
                within a large combinatorial space. A genetic algorithm is an iterative, population based technique
                to decrease the amount of sampling necessary for finding a good solution. In this GA, the population
                consists of candidate solutions, that are comprised of reactants (building blocks), which make up a
                compound.
            </p>
            <p>
                The building blocks for candidate solutions come from
                libraries of small fragments that can be joined into a molecule. For the fitness function however
                the potency has to be calculated from a three-dimensional (3D) compound docked in a protein
                pocket. Therefore, a pipeline was implemented that converts fragments from fragment libraries,
                into 3D compounds that are ready for docking. The pipeline consists of joining the fragments using
                ChemAxon Reactor,
                creating conformers with ChemAxon's Conformer Plugin, Aligning the conformers to an anchor using
                ChemAxon Alignment and
                docing with Moloc's Mol3d or Smina.
            </p>
            <p>
                The genotype of a candidate solution, or individual, is essentially a combination of one fragment from
                every reactant file supplied.
                Species in this application correspond to the group of candidate solutions that have their genotype
                reacted
                with the same reaction scheme.
            </p>
            <p>
                After the form is submitted, it might take a few seconds before the first populations are shown in the
                graph(s) below. You can terminate the genetic algorithm by hand using the terminate button.
            </p>
            <form name="compoundEvolverForm"
                  novalidate="novalidate"
                  enctype="multipart/form-data">
                <div class="alert alert-danger" ng-show="response.hasError" ng-bind="response.error"></div>
                <div class="card">
                    <h5 class="card-header"><b>Building blocks and reaction</b></h5>
                    <div id="file-input" class="card-body">
                        <p>
                            Through this form you are able to specify reactants, reaction schemes, and how the reactants
                            map to the reactions, a protein target and an anchor molecule. It is important that the
                            Reaction schemes work within ChemAxon Reactor according to your expectations. This means
                            that the rules you have in mind for reactions schemes (for example the rule that says a
                            fragment from library 'A' with a hydroxyl group
                            may not be incorporated) should be written in the reaction schemes themselves. It should
                            also
                            be clear that the same number of reactants must be applied to the reaction as the reaction
                            expects.
                            Ordering the reactants, which is also important, can be done via a button that appears after
                            reaction files are selected.
                        </p>
                        <p>
                            The anchor that is required is used by the computational pipeline to align conformers to,
                            and this should thus be located at the position where the conformers should be placed.
                        </p>
                        <div class="form-group row">
                            <label for="reaction-files" class="col-sm-3 col-form-label">Reaction files (.mrv)</label>
                            <div class="col-sm-9">
                                <div class="custom-file">
                                    <input type="file"
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
                                        <span ng-repeat="file in reactionFiles.files" id="reactions-label">{{file.name}} </span>
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
<%--                        <iframe id="sketch" data-reaction="BASIC" data-toolbars="reaction" src="marvin/editor.html"--%>
<%--                                style="overflow: hidden; min-width: 55vw; min-height: 40vh; border: 1px solid darkgray;"></iframe>--%>
<%--                        <button ng-click="doMarvin()">Click Me!</button>--%>
                        <div class="form-group row">
                            <label for="reactant-files" class="col-sm-3 col-form-label">Reactants files
                                (.smiles,
                                .smi)</label>
                            <div class="col-sm-9">
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
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="!reactantFiles.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </small>
                                <small class="form-text text-danger"
                                       ng-show="reactantFiles.wrongExtension">
                                    Only smiles files (.smiles, .smi) are accepted
                                </small>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="max-reactant-weight" class="col-sm-3 col-form-label">Maximum reactant weight (0 for no limit)</label>
                            <div class="col-sm-9">
                                <input type="number"
                                       class="form-control"
                                       ng-model="formModel.maxReactantWeight"
                                       id="max-reactant-weight"
                                       name="maxReactantWeight"
                                       required="required"
                                       min="0"
                                       ng-class="{
                    'is-invalid':!compoundEvolverForm.maxReactantWeight.$valid && (!compoundEvolverForm.maxReactantWeight.$pristine || compoundEvolverForm.$submitted),
                    'is-valid':compoundEvolverForm.maxReactantWeight.$valid && (!compoundEvolverForm.maxReactantWeight.$pristine || compoundEvolverForm.$submitted)}">
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="compoundEvolverForm.maxReactantWeight.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </small>
                                <small class="form-text text-danger"
                                       ng-show="(compoundEvolverForm.maxReactantWeight.$error.number || compoundEvolverForm.maxReactantWeight.$error.min) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A maximum weight of at least 0 is required
                                </small>
                                <small class="form-text">
                                    The maximum reactant weight is a filter on the reactants to remove very high weight compounds that won't yield useful results.
                                </small>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="smarts-filtering" class="col-sm-3 col-form-label">Smarts codes to filter reactants on</label>
                            <div class="col-sm-9">
                                <textarea
                                       class="form-control"
                                       ng-model="formModel.smartsFiltering"
                                       id="smarts-filtering"
                                       name="smartsFiltering"
                                       required="required"
                                       ng-class="{'is-valid':True}">
                                </textarea>
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text">
                                    The smarts code filter removes any reactants that match the specified smarts
                                </small>
                            </div>
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
                                           multiple="multiple"
                                           file-bind="formModel.receptorFile"
                                           ng-class="{
                    'is-invalid':receptorFile.wrongExtension || (!receptorFile.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)),
                    'is-valid':!receptorFile.wrongExtension && (!receptorFile.pristine || compoundEvolverForm.$submitted)}">
                                    <label class="custom-file-label overflow-hidden"
                                           for="receptor-file">
                                        <span ng-repeat="file in receptorFile.files">{{file.name}} </span>
                                        <span ng-hide="receptorFile.files.length">Choose receptor files</span>
                                    </label>
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
                                           multiple="multiple"
                                           file-bind="formModel.anchorFragmentFile"
                                           ng-class="{
                    'is-invalid':anchorFragmentFile.wrongExtension || (!anchorFragmentFile.hasFile && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)),
                    'is-valid':!anchorFragmentFile.wrongExtension && (!anchorFragmentFile.pristine || compoundEvolverForm.$submitted)}">
                                    <label class="custom-file-label overflow-hidden"
                                           for="anchor-fragment-file">
                                        <span ng-repeat="file in anchorFragmentFile.files">{{file.name}} </span>
                                        <span ng-hide="anchorFragmentFile.files.length">Choose anchor files</span>
                                    </label>
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
                    <h5 class="card-header"><b>Multiple reactions</b></h5>
                    <div id="multi-reaction-settings" class="card-body">
                        <p>
                            In case multiple reaction schemes are selected the following options have to be chosen.
                            In case a combination of reactants could work with either one of the reaction schemes (for
                            example,
                            if one of reactions uses only fragments from library 'A' with a hydroxyl group and the
                            other reaction uses only fragments from library 'A' without a hydroxyl group)
                            it makes the most sense to determine the species just by trying them all,
                            and to always perform crossover between species. If the reaction schemes are independent of
                            each other, it makes the most sense to have fixed species and not perform crossover.
                        </p>
                        <div class="form-group row">
                            <label for="species-determination-method" class="col-sm-3 col-form-label">Reaction
                                determination method
                            </label>
                            <div class="col-sm-9">
                                <select class="form-control"
                                        id="species-determination-method"
                                        ng-model="formModel.speciesDeterminationMethod"
                                        name="speciesDeterminationMethod"
                                        required="required">
                                    <option value="Dynamic">By reactants (try reactions until success)</option>
                                    <option value="Fixed">Fixed (reactions are assigned at random or in equal amounts)
                                    </option>
                                </select>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="interspecies-crossover-method" class="col-sm-3 col-form-label">Interspecies
                                crossover method
                            </label>
                            <div class="col-sm-9">
                                <select class="form-control"
                                        id="interspecies-crossover-method"
                                        ng-model="formModel.interspeciesCrossoverMethod"
                                        name="interspeciesCrossoverMethod"
                                        required="required">
                                    <option value="None">No not perform crossover between different species</option>
                                    <option value="Intersection">Perform crossover at genes used in both species
                                    </option>
                                    <option value="Complete">Always perform crossover between different species</option>
                                </select>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="card">
                    <h5 class="card-header"><b>Genetic operators</b></h5>
                    <div id="operator-settings" class="card-body">
                        <p>
                            The genetic operators affect the progress and overall behaviour of the genetic algorithm.
                            The wrong settings might cause convergence to a local optimum or a randomized search.
                        </p>
                        <div class="form-group row">
                            <label for="generation-size" class="col-sm-3 col-form-label">Population size</label>
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
                                <small class="form-text">
                                    The generation or population size represents the amount of
                                    candidates that will be produced and scored in each generation.
                                    This must be at least two.
                                </small>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="number-of-generations" class="col-sm-3 col-form-label">Maximum number of
                                generations</label>
                            <div class="col-sm-9">
                                <input type="number"
                                       class="form-control"
                                       ng-model="formModel.numberOfGenerations"
                                       id="number-of-generations"
                                       name="numberOfGenerations"
                                       required="required"
                                       min="0"
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
                                    An integer value (a whole number) more or equal to 0 is required
                                </small>
                                <small class="form-text">
                                Evolution will terminate when the maximum number of generations is reached.
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
                                <small class="form-text">
                                The selection size represents the portion of candidates that are selected for
                                a next generation through crossover or elitism. The other candidates are 'killed'.
                                </small>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="set-adaptive" class="col-sm-3 col-form-label">Adaptive GA
                            </label>
                            <div class="col-sm-9">
                                <div class="form-check">
                                    <input type="checkbox"
                                           class="form-check-input"
                                           ng-model="formModel.setAdaptive"
                                           id="set-adaptive"
                                           name="setAdaptive"
                                           value="adaptive">
                                    <label class="form-check-label" for="set-adaptive">
                                        Use an adaptive genetic algorithm
                                    </label>
                                </div>
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
                                       ng-disabled="formModel.setAdaptive"
                                       ng-class="{
                    'is-invalid':!compoundEvolverForm.crossoverRate.$valid && !formModel.setAdaptive && (!compoundEvolverForm.crossoverRate.$pristine || compoundEvolverForm.$submitted),
                    'is-valid':compoundEvolverForm.crossoverRate.$valid && !formModel.setAdaptive && (!compoundEvolverForm.crossoverRate.$pristine || compoundEvolverForm.$submitted)}">
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
                                <small class="form-text">
                                The crossover rate represents the probability of crossover being performed
                                for producing a new candidate relative to the elitism and random immigrant rates.
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
                                <small class="form-text">
                                The elitism rate represents the probability of the elitism strategy being carried
                                out for producing a new candidate relative to the
                                crossover and random immigrant rates.
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
                                       ng-disabled="formModel.setAdaptive"
                                       ng-class="{
                    'is-invalid':!compoundEvolverForm.mutationRate.$valid && !formModel.setAdaptive && (!compoundEvolverForm.mutationRate.$pristine || compoundEvolverForm.$submitted),
                    'is-valid':compoundEvolverForm.mutationRate.$valid && !formModel.setAdaptive && (!compoundEvolverForm.mutationRate.$pristine || compoundEvolverForm.$submitted)}">
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
                                <small class="form-text">
                                The mutation rate represents the probability of a single gene being mutated.
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
                                <small class="form-text">
                                The random immigrant rate represents the probability of a random immigrant being made
                                for producing a new candidate relative to the elitism and crossover rates.
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
                                <small class="form-text">
                                    The selection method affects the manner in which candidates are selected. Fitness
                                    proportionate selection or roulette wheel selection picks candidates by their
                                    relative probability, based on the fitness score. Truncated selection picks
                                    only the best individuals, exerting big selective pressure. Tournament selection
                                    picks the best individual from two randomly selected individuals.
                                </small>
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
                                <small class="form-text">
                                    The mutation method applies mutations to single genes. The distance independent
                                    mutation method does this by picking a random allele, or reactant, as a substitution
                                    with the set mutation rate. The distance dependent method takes the similarity of
                                    alleles into account. More similar alleles have a higher chance of being chosen as
                                    a substitution.
                                </small>
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
                                <small class="form-text">
                                    The termination condition specifies under what condition the evolution should be
                                    terminated. When the maximum number of generations is reached evolution is always
                                    terminated, also with the convergence reached condition set.
                                </small>
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
                        <p>
                            Here you should set the number of conformers that is required for the specific experiment.
                            With your experience and knowledge about the specific experiment it is most likely that you
                            are able to provide a realistic number for the necessary amount of three-dimensional
                            conformers.
                            I should mention that a more restricting protein will result in a lot of conformers being
                            removed
                            for clashing.
                        </p>
                        <P>
                            The docking program has a great impact on the score of the candidate solutions. Smina
                            calculates
                            binding affinity in kcal/mol, while Moloc's Mol3d uses an arbitrary scale. Smina is also
                            considerably faster than Mol3d.
                        </P>
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
                    'is-invalid':!compoundEvolverForm.conformerCount.$valid && (!compoundEvolverForm.conformerCount.$pristine || compoundEvolverForm.$submitted),
                    'is-valid':compoundEvolverForm.conformerCount.$valid && (!compoundEvolverForm.conformerCount.$pristine || compoundEvolverForm.$submitted)}">
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="compoundEvolverForm.conformerCount.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </small>
                                <small class="form-text text-danger"
                                       ng-show="(compoundEvolverForm.conformerCount.$error.number || compoundEvolverForm.conformerCount.$error.step || compoundEvolverForm.conformerCount.$error.min || compoundEvolverForm.conformerCount.$error.max) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
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
                            <label for="scoring-option" class="col-sm-3 col-form-label">Scoring option</label>
                            <div class="col-sm-9">
                                <select class="form-control"
                                        id="scoring-option"
                                        ng-model="formModel.scoringOption"
                                        name="scoringOption"
                                        required="required">
                                    <option value="smina">smina</option>
                                    <option value="mab">mab(moloc)</option>
                                    <option value="scorpion">Scorpion</option>
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
                                    <option value="ligandLipophilicityEfficiency">Ligand lipophilic efficiency</option>
                                </select>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="card">
                    <h5 class="card-header"><b>Filters</b></h5>
                    <div id="filter-settings" class="card-body">
                        <p>Here a couple of options can be set for filtering the results. The RMSD in Ångström between
                            the anchor
                            and anchor matching structure in the minimized conformer is calculated to determine if the
                            minimized structure deviates to much from the anchor. The receptor exclusion shape, which is
                            based on the Pharmit exclusion shape, approximates the solvent excluded surface of the
                            receptor
                            within a grid like data structure wherein one Ångström corresponds with 2 cubes. A negative
                            tolerance
                            value will add the additive inverse of the tolerance value, rounded up to the nearest half
                            an Ångström,
                            to the shape. A positive tolerance
                            will remove the tolerance value from the shape rounded up to the nearest half an
                            Ångström</p>
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
                            <label for="exclusion-shape-tolerance" class="col-sm-3 col-form-label">
                                Receptor exclusion shape tolerance
                            </label>
                            <div class="col-sm-9">
                                <input type="number"
                                       class="form-control"
                                       ng-model="formModel.exclusionShapeTolerance"
                                       id="exclusion-shape-tolerance"
                                       name="exclusionShapeTolerance"
                                       required="required"
                                       min="-2"
                                       max="2"
                                       ng-class="{
                    'is-invalid':!compoundEvolverForm.exclusionShapeTolerance.$valid && (!compoundEvolverForm.exclusionShapeTolerance.$pristine || compoundEvolverForm.$submitted),
                    'is-valid':compoundEvolverForm.exclusionShapeTolerance.$valid && (!compoundEvolverForm.exclusionShapeTolerance.$pristine || compoundEvolverForm.$submitted)}">
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="compoundEvolverForm.exclusionShapeTolerance.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </small>
                                <small class="form-text text-danger"
                                       ng-show="(compoundEvolverForm.exclusionShapeTolerance.$error.number || compoundEvolverForm.exclusionShapeTolerance.$error.min || compoundEvolverForm.exclusionShapeTolerance.$error.max) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A value below -2 or above 2 is forbidden
                                </small>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="allow-duplicates" class="col-sm-3 col-form-label">Duplicate candidates
                            </label>
                            <div class="col-sm-9">
                                <div class="form-check">
                                    <input type="checkbox"
                                           class="form-check-input"
                                           ng-model="formModel.allowDuplicates"
                                           id="allow-duplicates"
                                           name="allowDuplicates"
                                           value="allow-duplicates">
                                    <label class="form-check-label" for="allow-duplicates">
                                        Allow duplicates within generations
                                    </label>
                                </div>
                            </div>
                        </div>
                        <div class="form-group row">
                            <label for="use-lipinski" class="col-sm-3 col-form-label">Filer application
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
                        <div class="form-group row">
                            <label for="min-qed" class="col-sm-3 col-form-label">
                                Minimum QED (Quantitative Estimate of Drug-likeness, see also the relevant <a href="https://www.doi.org/10.1038/nchem.1243">paper</a>)
                            </label>
                            <div class="col-sm-9">
                                <input type="number"
                                       class="form-control"
                                       ng-model="formModel.minQED"
                                       id="min-qed"
                                       name="minQED"
                                       required="required"
                                       min="0"
                                       max="1"
                                       ng-class="{
                    'is-invalid':!compoundEvolverForm.minQED.$valid && (!compoundEvolverForm.minQED.$pristine || compoundEvolverForm.$submitted),
                    'is-valid':compoundEvolverForm.minQED.$valid && (!compoundEvolverForm.minQED.$pristine || compoundEvolverForm.$submitted)}">
                            </div>
                            <div class="col-sm-9 offset-sm-3">
                                <small class="form-text text-danger"
                                       ng-show="compoundEvolverForm.minQED.$error.required && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    This field is required
                                </small>
                                <small class="form-text text-danger"
                                       ng-show="(compoundEvolverForm.minQED.$error.number || compoundEvolverForm.minQED.$error.min || compoundEvolverForm.minQED.$error.max) && (!compoundEvolverForm.$pristine || compoundEvolverForm.$submitted)">
                                    A value below 0 or above 1 is forbidden
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
                                <button class="btn btn-primary"
                                        ng-click="setFrequentUpdateInterval()">
                                    Submit
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
                    <a class="nav-link" href ng-click="downloadSelectedGeneration()">Download selected generation</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href ng-click="downloadMultiSdf(1)">best candidates per generation</a>
                </li>
                <li class="nav-item">
                    <div class="dropdown">
                        <button class="btn btn-link dropdown-toggle" type="button" id="dropdownMenuButton" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                            Download all candidates
                        </button>
                        <div class="dropdown-menu" aria-labelledby="dropdownMenuButton">
                            <a class="dropdown-item" href ng-click="downloadRun()">Complete compressed zip</a>
                            <a class="dropdown-item" href ng-click="downloadMultiSdf(0)">Multi-sdf of best conformers</a>
                        </div>
                    </div>
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
                    <td>{{showSmiles(candidate.smiles, $index)}}
                        <canvas id="{{$index}}" width="300" height="200"></canvas>
                    </td>
                    <td>{{candidate.rawScore | number:4}}</td>
                    <td>{{candidate.ligandEfficiency | number:4}}</td>
                    <td>{{candidate.ligandLipophilicityEfficiency | number:4}}</td>
                </tr>
                </tbody>
            </table>
            <div id="viewport_best" style="width:15vw; height:20vh; display:inline-block;">Best compound of selected generation</div>
            <div id="viewport_avg" style="width:15vw; height:20vh; display:inline-block;">Average (Median) compound of selected generation</div>
            <div id="viewport_worst" style="width:15vw; height:20vh; display:inline-block;">Worst compound of selected generation</div>


        </div>
    </div>
</div>
</body>
</html>
