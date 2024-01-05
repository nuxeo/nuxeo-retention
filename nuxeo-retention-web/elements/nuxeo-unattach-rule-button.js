/**
(C) Copyright Nuxeo Corp. (http://nuxeo.com/)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
import { mixinBehaviors } from '@polymer/polymer/lib/legacy/class.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
import { FiltersBehavior } from '@nuxeo/nuxeo-ui-elements/nuxeo-filters-behavior.js';
import { FormatBehavior } from '@nuxeo/nuxeo-ui-elements/nuxeo-format-behavior.js';
import '@nuxeo/nuxeo-elements/nuxeo-operation.js';
import '@nuxeo/nuxeo-ui-elements/widgets/nuxeo-tooltip.js';
import '@polymer/paper-icon-button/paper-icon-button.js';

/**
`nuxeo-unattach-rule-button`
@group Nuxeo UI
@element nuxeo-unattach-rule-button
*/

class RetentionUnattachRuleButton extends mixinBehaviors([FiltersBehavior, FormatBehavior], Nuxeo.Element) {
  static get template() {
    return html`
      <style include="nuxeo-action-button-styles"></style>

      <nuxeo-operation id="unretainOp" op="Document.UnattachRetentionRule" input="[[document]]"> </nuxeo-operation>

      <dom-if if="[[_isAvailable(document)]]">
        <template>
          <div class="action" on-click="_unretain">
            <paper-icon-button icon="[[icon]]" noink=""></paper-icon-button>
            <span class="label" hidden$="[[!showLabel]]">[[_label]]</span>
            <nuxeo-tooltip>[[_label]]</nuxeo-tooltip>
          </div>
        </template>
      </dom-if>
    `;
  }

  static get is() {
    return 'nuxeo-unattach-rule-button';
  }

  static get properties() {
    return {
      /**
       * Input document.
       */
      document: Object,

      /**
       * Icon to use (iconset_name:icon_name).
       */
      icon: {
        type: String,
        value: 'nuxeo:unretain',
      },

      /**
       * `true` if the action should display the label, `false` otherwise.
       */
      showLabel: {
        type: Boolean,
        value: false,
      },

      _label: {
        type: String,
        computed: '_computeLabel(i18n)',
      },
    };
  }

  _isAvailable(document) {
    return (
      document &&
      document.isFlexibleRecord &&
      this.hasFacet(document, 'Record') &&
      this.hasPermission(document, 'WriteProperties') &&
      this.hasPermission(document, 'UnsetRetention')
    );
  }

  _computeLabel() {
    return this.i18n('retention.rule.label.undeclare');
  }

  _unretain() {
    this.$.unretainOp.execute().then(() => {
      this.dispatchEvent(
        new CustomEvent('document-updated', {
          composed: true,
          bubbles: true,
        }),
      );
      this.dispatchEvent(
        new CustomEvent('notify', {
          composed: true,
          bubbles: true,
          detail: { message: this.i18n('retention.rule.label.undeclared.notify') },
        }),
      );
    });
  }
}
customElements.define(RetentionUnattachRuleButton.is, RetentionUnattachRuleButton);
