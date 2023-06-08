/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.client.console.tasks;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.commons.RealmsUtils;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxSearchFieldPanel;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxSpinnerFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.MacroTaskTO;
import org.apache.syncope.common.lib.to.ProvisioningTaskTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.PullMode;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.ThreadPoolSettings;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.springframework.beans.PropertyAccessorFactory;

public class SchedTaskWizardBuilder<T extends SchedTaskTO> extends BaseAjaxWizardBuilder<T> {

    private static final long serialVersionUID = 5945391813567245081L;

    protected final TaskType type;

    protected final RealmRestClient realmRestClient;

    protected final TaskRestClient taskRestClient;

    protected PushTaskWrapper wrapper;

    protected CrontabPanel crontabPanel;

    protected final boolean fullRealmsTree;

    public SchedTaskWizardBuilder(
            final TaskType type,
            final T taskTO,
            final RealmRestClient realmRestClient,
            final TaskRestClient taskRestClient,
            final PageReference pageRef) {

        super(taskTO, pageRef);
        this.type = type;
        this.realmRestClient = realmRestClient;
        this.taskRestClient = taskRestClient;
        this.fullRealmsTree = SyncopeWebApplication.get().fullRealmsTree(realmRestClient);
    }

    @Override
    protected Serializable onApplyInternal(final SchedTaskTO modelObject) {
        if (modelObject instanceof PushTaskTO && wrapper != null) {
            wrapper.fillFilterConditions();
        }

        modelObject.setCronExpression(crontabPanel.getCronExpression());
        if (modelObject.getKey() == null) {
            taskRestClient.create(type, modelObject);
        } else {
            taskRestClient.update(type, modelObject);
        }
        return modelObject;
    }

    @Override
    protected WizardModel buildModelSteps(final SchedTaskTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Profile(modelObject));
        if (modelObject instanceof PushTaskTO) {
            wrapper = new PushTaskWrapper(PushTaskTO.class.cast(modelObject));
            wizardModel.add(new PushTaskFilters(wrapper, pageRef));
        }
        wizardModel.add(new Schedule(modelObject));
        return wizardModel;
    }

    protected List<String> searchRealms(final String realmQuery) {
        return realmRestClient.search(fullRealmsTree
                ? RealmsUtils.buildRootQuery()
                : RealmsUtils.buildKeywordQuery(realmQuery)).
                getResult().stream().map(RealmTO::getFullPath).collect(Collectors.toList());
    }

    public class Profile extends WizardStep {

        private static final long serialVersionUID = -3043839139187792810L;

        private final IModel<List<String>> taskJobDelegates = SyncopeWebApplication.get().
                getImplementationInfoProvider().getTaskJobDelegates();

        private final IModel<List<String>> reconFilterBuilders = SyncopeWebApplication.get().
                getImplementationInfoProvider().getReconFilterBuilders();

        private final IModel<List<String>> pullActions = SyncopeWebApplication.get().
                getImplementationInfoProvider().getPullActions();

        private final IModel<List<String>> pushActions = SyncopeWebApplication.get().
                getImplementationInfoProvider().getPushActions();

        public Profile(final SchedTaskTO taskTO) {
            AjaxTextFieldPanel name = new AjaxTextFieldPanel(
                    Constants.NAME_FIELD_NAME, Constants.NAME_FIELD_NAME,
                    new PropertyModel<>(taskTO, Constants.NAME_FIELD_NAME),
                    false);
            name.addRequiredLabel();
            name.setEnabled(true);
            add(name);

            AjaxTextFieldPanel description = new AjaxTextFieldPanel(
                    Constants.DESCRIPTION_FIELD_NAME, Constants.DESCRIPTION_FIELD_NAME,
                    new PropertyModel<>(taskTO, Constants.DESCRIPTION_FIELD_NAME), false);
            description.setEnabled(true);
            add(description);

            AjaxCheckBoxPanel active = new AjaxCheckBoxPanel(
                    "active", "active", new PropertyModel<>(taskTO, "active"), false);
            add(active);

            AjaxDropDownChoicePanel<String> jobDelegate = new AjaxDropDownChoicePanel<>(
                    "jobDelegate", "jobDelegate", new PropertyModel<>(taskTO, "jobDelegate"), false);
            jobDelegate.setChoices(taskJobDelegates.getObject());
            jobDelegate.addRequiredLabel();
            jobDelegate.setEnabled(taskTO.getKey() == null);
            add(jobDelegate);

            AutoCompleteSettings settings = new AutoCompleteSettings();
            settings.setShowCompleteListOnFocusGain(fullRealmsTree);
            settings.setShowListOnEmptyInput(fullRealmsTree);

            // ------------------------------
            // Only for macro tasks
            // ------------------------------            
            WebMarkupContainer macroTaskSpecifics = new WebMarkupContainer("macroTaskSpecifics");
            add(macroTaskSpecifics.setRenderBodyOnly(true));

            AjaxSearchFieldPanel realm =
                    new AjaxSearchFieldPanel("realm", "realm",
                            new PropertyModel<>(taskTO, "realm"), settings) {

                private static final long serialVersionUID = -6390474600233486704L;

                @Override
                protected Iterator<String> getChoices(final String input) {
                    return (RealmsUtils.checkInput(input)
                            ? searchRealms(input)
                            : List.<String>of()).iterator();
                }
            };

            if (taskTO instanceof MacroTaskTO) {
                realm.addRequiredLabel();
                if (StringUtils.isBlank(MacroTaskTO.class.cast(taskTO).getRealm())) {
                    // add a default destination realm if missing in the task
                    realm.setModelObject(SyncopeConstants.ROOT_REALM);
                }
            }
            macroTaskSpecifics.add(realm);

            AjaxCheckBoxPanel continueOnError = new AjaxCheckBoxPanel(
                    "continueOnError", "continueOnError", new PropertyModel<>(taskTO, "continueOnError"), false);
            macroTaskSpecifics.add(continueOnError);

            AjaxCheckBoxPanel saveExecs = new AjaxCheckBoxPanel(
                    "saveExecs", "saveExecs", new PropertyModel<>(taskTO, "saveExecs"), false);
            macroTaskSpecifics.add(saveExecs);

            // ------------------------------
            // Only for pull tasks
            // ------------------------------            
            WebMarkupContainer pullTaskSpecifics = new WebMarkupContainer("pullTaskSpecifics");
            add(pullTaskSpecifics.setRenderBodyOnly(true));

            boolean isFiltered = false;
            if (taskTO instanceof PullTaskTO) {
                isFiltered = PullTaskTO.class.cast(taskTO).getPullMode() == PullMode.FILTERED_RECONCILIATION;
            } else {
                pullTaskSpecifics.setEnabled(false).setVisible(false);
            }

            AjaxDropDownChoicePanel<PullMode> pullMode = new AjaxDropDownChoicePanel<>(
                    "pullMode", "pullMode", new PropertyModel<>(taskTO, "pullMode"), false);
            pullMode.setChoices(List.of(PullMode.values()));
            if (taskTO instanceof PullTaskTO) {
                pullMode.addRequiredLabel();
            }
            pullMode.setNullValid(!(taskTO instanceof PullTaskTO));
            pullTaskSpecifics.add(pullMode);

            AjaxDropDownChoicePanel<String> reconFilterBuilder = new AjaxDropDownChoicePanel<>(
                    "reconFilterBuilder", "reconFilterBuilder",
                    new PropertyModel<>(taskTO, "reconFilterBuilder"), false);
            reconFilterBuilder.setChoices(reconFilterBuilders.getObject());
            reconFilterBuilder.setEnabled(isFiltered);
            reconFilterBuilder.setRequired(isFiltered);
            pullTaskSpecifics.add(reconFilterBuilder);

            pullMode.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    reconFilterBuilder.setEnabled(
                            pullMode.getModelObject() == PullMode.FILTERED_RECONCILIATION);
                    reconFilterBuilder.setRequired(
                            pullMode.getModelObject() == PullMode.FILTERED_RECONCILIATION);
                    target.add(reconFilterBuilder);
                }
            });

            AjaxSearchFieldPanel destinationRealm =
                    new AjaxSearchFieldPanel("destinationRealm", "destinationRealm",
                            new PropertyModel<>(taskTO, "destinationRealm"), settings) {

                private static final long serialVersionUID = -6390474600233486704L;

                @Override
                protected Iterator<String> getChoices(final String input) {
                    return (RealmsUtils.checkInput(input)
                            ? searchRealms(input)
                            : List.<String>of()).iterator();
                }
            };

            if (taskTO instanceof PullTaskTO) {
                destinationRealm.addRequiredLabel();
                if (StringUtils.isBlank(PullTaskTO.class.cast(taskTO).getDestinationRealm())) {
                    // add a default destination realm if missing in the task
                    destinationRealm.setModelObject(SyncopeConstants.ROOT_REALM);
                }
            }
            pullTaskSpecifics.add(destinationRealm);

            AjaxCheckBoxPanel remediation = new AjaxCheckBoxPanel(
                    "remediation", "remediation", new PropertyModel<>(taskTO, "remediation"), false);
            pullTaskSpecifics.add(remediation);

            // ------------------------------
            // Only for push tasks
            // ------------------------------
            WebMarkupContainer pushTaskSpecifics = new WebMarkupContainer("pushTaskSpecifics");
            add(pushTaskSpecifics.setRenderBodyOnly(true));

            if (!(taskTO instanceof PushTaskTO)) {
                pushTaskSpecifics.setEnabled(false).setVisible(false);
            }

            AjaxSearchFieldPanel sourceRealm = new AjaxSearchFieldPanel(
                    "sourceRealm", "sourceRealm", new PropertyModel<>(taskTO, "sourceRealm"), settings) {

                private static final long serialVersionUID = -6390474600233486704L;

                @Override
                protected Iterator<String> getChoices(final String input) {
                    return (RealmsUtils.checkInput(input)
                            ? searchRealms(input)
                            : List.<String>of()).iterator();
                }
            };

            if (taskTO instanceof PushTaskTO) {
                sourceRealm.addRequiredLabel();
            }
            pushTaskSpecifics.add(sourceRealm);

            // ------------------------------
            // For push and pull tasks
            // ------------------------------
            WebMarkupContainer provisioningTaskSpecifics = new WebMarkupContainer("provisioningTaskSpecifics");
            add(provisioningTaskSpecifics.setOutputMarkupId(true));

            if (taskTO instanceof ProvisioningTaskTO) {
                jobDelegate.setEnabled(false).setVisible(false);
                macroTaskSpecifics.setEnabled(false).setVisible(false);
            } else if (taskTO instanceof MacroTaskTO) {
                jobDelegate.setEnabled(false).setVisible(false);
                provisioningTaskSpecifics.setEnabled(false).setVisible(false);
            } else {
                provisioningTaskSpecifics.setEnabled(false).setVisible(false);
                macroTaskSpecifics.setEnabled(false).setVisible(false);
            }

            AjaxPalettePanel<String> actions = new AjaxPalettePanel.Builder<String>().
                    setAllowMoveAll(true).setAllowOrder(true).
                    build("actions",
                            new PropertyModel<>(taskTO, "actions"),
                            new ListModel<>(taskTO instanceof PushTaskTO
                                    ? pushActions.getObject() : pullActions.getObject()));
            provisioningTaskSpecifics.add(actions.setOutputMarkupId(true));

            AjaxDropDownChoicePanel<MatchingRule> matchingRule = new AjaxDropDownChoicePanel<>(
                    "matchingRule", "matchingRule", new PropertyModel<>(taskTO, "matchingRule"), false);
            matchingRule.setChoices(List.of(MatchingRule.values()));
            provisioningTaskSpecifics.add(matchingRule);

            AjaxDropDownChoicePanel<UnmatchingRule> unmatchingRule = new AjaxDropDownChoicePanel<>(
                    "unmatchingRule", "unmatchingRule", new PropertyModel<>(taskTO, "unmatchingRule"),
                    false);
            unmatchingRule.setChoices(List.of(UnmatchingRule.values()));
            provisioningTaskSpecifics.add(unmatchingRule);

            AjaxCheckBoxPanel performCreate = new AjaxCheckBoxPanel(
                    "performCreate", "performCreate", new PropertyModel<>(taskTO, "performCreate"), false);
            provisioningTaskSpecifics.add(performCreate);

            AjaxCheckBoxPanel performUpdate = new AjaxCheckBoxPanel(
                    "performUpdate", "performUpdate", new PropertyModel<>(taskTO, "performUpdate"), false);
            provisioningTaskSpecifics.add(performUpdate);

            AjaxCheckBoxPanel performDelete = new AjaxCheckBoxPanel(
                    "performDelete", "performDelete", new PropertyModel<>(taskTO, "performDelete"), false);
            provisioningTaskSpecifics.add(performDelete);

            AjaxCheckBoxPanel syncStatus = new AjaxCheckBoxPanel(
                    "syncStatus", "syncStatus", new PropertyModel<>(taskTO, "syncStatus"), false);
            provisioningTaskSpecifics.add(syncStatus);

            // Concurrent settings
            PropertyModel<ThreadPoolSettings> concurrentSettingsModel =
                    new PropertyModel<>(taskTO, "concurrentSettings");

            AjaxCheckBoxPanel enableConcurrentSettings = new AjaxCheckBoxPanel(
                    "enableConcurrentSettings", "enableConcurrentSettings", new IModel<Boolean>() {

                private static final long serialVersionUID = -7126718045816207110L;

                @Override
                public Boolean getObject() {
                    return concurrentSettingsModel.getObject() != null;
                }

                @Override
                public void setObject(final Boolean object) {
                    // nothing to do
                }
            });
            provisioningTaskSpecifics.add(enableConcurrentSettings.
                    setVisible(taskTO instanceof ProvisioningTaskTO).setOutputMarkupId(true));

            FieldPanel<Integer> corePoolSize = new AjaxSpinnerFieldPanel.Builder<Integer>().min(1).build(
                    "corePoolSize",
                    "corePoolSize",
                    Integer.class,
                    new ConcurrentSettingsValueModel(concurrentSettingsModel, "corePoolSize")).setRequired(true);
            corePoolSize.setOutputMarkupPlaceholderTag(true).setOutputMarkupId(true);
            corePoolSize.setVisible(taskTO instanceof ProvisioningTaskTO
                    ? concurrentSettingsModel.getObject() != null
                    : false);
            provisioningTaskSpecifics.add(corePoolSize);

            FieldPanel<Integer> maxPoolSize = new AjaxSpinnerFieldPanel.Builder<Integer>().min(1).build(
                    "maxPoolSize",
                    "maxPoolSize",
                    Integer.class,
                    new ConcurrentSettingsValueModel(concurrentSettingsModel, "maxPoolSize")).setRequired(true);
            maxPoolSize.setOutputMarkupPlaceholderTag(true).setOutputMarkupId(true);
            maxPoolSize.setVisible(taskTO instanceof ProvisioningTaskTO
                    ? concurrentSettingsModel.getObject() != null
                    : false);
            provisioningTaskSpecifics.add(maxPoolSize);

            FieldPanel<Integer> queueCapacity = new AjaxSpinnerFieldPanel.Builder<Integer>().min(1).build(
                    "queueCapacity",
                    "queueCapacity",
                    Integer.class,
                    new ConcurrentSettingsValueModel(concurrentSettingsModel, "queueCapacity")).setRequired(true);
            queueCapacity.setOutputMarkupPlaceholderTag(true).setOutputMarkupId(true);
            queueCapacity.setVisible(taskTO instanceof ProvisioningTaskTO
                    ? concurrentSettingsModel.getObject() != null
                    : false);
            provisioningTaskSpecifics.add(queueCapacity);

            enableConcurrentSettings.getField().add(
                    new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    if (concurrentSettingsModel.getObject() == null) {
                        concurrentSettingsModel.setObject(new ThreadPoolSettings());
                    } else {
                        concurrentSettingsModel.setObject(null);
                    }

                    corePoolSize.setVisible(concurrentSettingsModel.getObject() != null);
                    maxPoolSize.setVisible(concurrentSettingsModel.getObject() != null);
                    queueCapacity.setVisible(concurrentSettingsModel.getObject() != null);

                    target.add(provisioningTaskSpecifics);
                }
            });
        }
    }

    protected static class ConcurrentSettingsValueModel implements IModel<Integer> {

        private static final long serialVersionUID = 8869612332790116116L;

        private final PropertyModel<ThreadPoolSettings> concurrentSettingsModel;

        private final String property;

        public ConcurrentSettingsValueModel(
                final PropertyModel<ThreadPoolSettings> concurrentSettingsModel,
                final String property) {

            this.concurrentSettingsModel = concurrentSettingsModel;
            this.property = property;
        }

        @Override
        public Integer getObject() {
            return Optional.ofNullable(concurrentSettingsModel.getObject()).
                    map(s -> (Integer) PropertyAccessorFactory.forBeanPropertyAccess(s).getPropertyValue(property)).
                    orElse(null);
        }

        @Override
        public void setObject(final Integer object) {
            Optional.ofNullable(concurrentSettingsModel.getObject()).
                    ifPresent(s -> PropertyAccessorFactory.forBeanPropertyAccess(s).setPropertyValue(property, object));
        }
    }

    public class Schedule extends WizardStep {

        private static final long serialVersionUID = -785981096328637758L;

        public Schedule(final SchedTaskTO taskTO) {
            crontabPanel = new CrontabPanel(
                    "schedule", new PropertyModel<>(taskTO, "cronExpression"), taskTO.getCronExpression());
            add(crontabPanel);
        }
    }
}
