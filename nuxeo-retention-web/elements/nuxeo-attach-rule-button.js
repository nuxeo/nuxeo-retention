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
import '@nuxeo/nuxeo-ui-elements/widgets/nuxeo-dialog.js';
import '@nuxeo/nuxeo-ui-elements/widgets/nuxeo-document-suggestion.js';
import '@nuxeo/nuxeo-ui-elements/widgets/nuxeo-tooltip.js';
import '@polymer/paper-button/paper-button.js';
import '@polymer/paper-dialog-scrollable/paper-dialog-scrollable.js';
import '@polymer/paper-icon-button/paper-icon-button.js';

/**
`nuxeo-attach-rule-button`
@group Nuxeo UI
@element nuxeo-attach-rule-button
*/
class RetentionAttachRuleButton extends mixinBehaviors([FiltersBehavior, FormatBehavior], Nuxeo.Element) {
  static get template() {
    return html`
      <style include="nuxeo-styles nuxeo-action-button-styles">
        /* Fix known stacking issue in iOS (NXP-24600)
          https://github.com/PolymerElements/paper-dialog-scrollable/issues/72 */
        paper-dialog-scrollable {
          --paper-dialog-scrollable: {
            -webkit-overflow-scrolling: auto;
          }
        }
      </style>

      <nuxeo-operation id="waitEs" op="Elasticsearch.WaitForIndexing" params='{ "timeoutSecond": 5, "refresh": true }'>
      </nuxeo-operation>
      <nuxeo-operation id="attachRuleOp" on-poll-start="_onPollStart" on-response="_onResponse"> </nuxeo-operation>

      <dom-if if="[[_isAvailable(provider, document)]]">
        <template>
          <div class="action" on-click="_toggleDialog">
            <paper-icon-button icon="[[icon]]" noink=""></paper-icon-button>
            <span class="label" hidden$="[[!showLabel]]">[[_label]]</span>
            <nuxeo-tooltip>[[_label]]</nuxeo-tooltip>
          </div>
        </template>
      </dom-if>

      <nuxeo-dialog id="dialog" with-backdrop="" no-auto-focus="">
        <h2>[[i18n('retention.rule.attachButton.label.heading')]]</h2>
        <paper-dialog-scrollable>
          <nuxeo-document-suggestion
            id="select"
            required=""
            label="[[i18n('retention.rule.attachButton.label.select')]]"
            placeholder="[[i18n('retention.rule.attachButton.label.placeholder')]]"
            selected-item="{{rule}}"
            min-chars="0"
            query-results-filter="[[_filterRules]]"
            result-formatter="[[ruleResultFormatter]]"
            selection-formatter="[[ruleSelectionFormatter]]"
            page-provider="manual_retention_rule_suggestion"
            repository="[[document.repository]]"
          >
          </nuxeo-document-suggestion>
        </paper-dialog-scrollable>
        <div class="buttons">
          <paper-button dialog-dismiss="" class="secondary">[[i18n('command.close')]]</paper-button>
          <paper-button name="add" class="primary" on-tap="_attach" disabled$="[[!_isValid(provider, document, rule)]]">
            [[i18n('retention.rule.attachButton.label')]]
          </paper-button>
        </div>
      </nuxeo-dialog>
    `;
  }

  static get is() {
    return 'nuxeo-attach-rule-button';
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
        value: 'nuxeo:attach-rule',
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

      // Dirty post filtering but hey! It's not like we can do a OR in NXQL
      _filterRules: {
        type: Function,
        value() {
          return this._filterRules.bind(this);
        },
      },

      /**
       * Rule.
       */
      rule: Object,

      /**
       * Page provider from which results are to be attached.
       */
      provider: {
        type: Object,
      },

      /**
       * Formatter for a suggested rule.
       */
      ruleResultFormatter: {
        type: Function,
        value() {
          return this._ruleResultFormatter.bind(this);
        },
      },

      /**
       * Formatter for a selected rule.
       */
      ruleSelectionFormatter: {
        type: Function,
        value() {
          return this._ruleSelectionFormatter.bind(this);
        },
      },
    };
  }

  _isAvailable() {
    return this.provider || this.canSetRetention(this.document);
  }

  _computeLabel() {
    return this.i18n('retention.rule.attachButton.label.heading');
  }

  _toggleDialog() {
    this.set('rule', undefined);
    this.$.dialog.toggle();
  }

  _attach() {
    if (this.provider) {
      this.$.attachRuleOp.op = 'Bulk.RunAction';
      this.$.attachRuleOp.input = this.provider;
      this.$.attachRuleOp.async = true;
      this.$.attachRuleOp.params = {
        action: 'attachRetentionRule',
        parameters: JSON.stringify({ ruleId: this.rule.uid }),
      };
      this.$.attachRuleOp.execute().then(() => this._toggleDialog());
    } else {
      this.$.attachRuleOp.op = 'Retention.AttachRule';
      this.$.attachRuleOp.input = this.document;
      this.$.attachRuleOp.async = false;
      this.$.attachRuleOp.params = { rule: this.rule.uid };
      this.$.attachRuleOp.execute().then(() => {
        this.dispatchEvent(
          new CustomEvent('document-updated', {
            composed: true,
            bubbles: true,
          }),
        );
        this._toggleDialog();
      });
    }
  }

  _onPollStart() {
    this.dispatchEvent(
      new CustomEvent('notify', {
        composed: true,
        bubbles: true,
        detail: { message: this.i18n('retention.rule.attachButton.bulk.poll') },
      }),
    );
  }

  _onResponse() {
    this.$.waitEs.execute().then(() => {
      this.dispatchEvent(
        new CustomEvent('notify', {
          composed: true,
          bubbles: true,
          detail: { message: this.i18n('retention.rule.attachButton.attached') },
        }),
      );
      this.dispatchEvent(
        new CustomEvent('refresh', {
          composed: true,
          bubbles: true,
        }),
      );
    });
  }

  _isValid() {
    return (this.document || this.provider) && this.rule;
  }

  _filterRules(rule) {
    if (this.provider) {
      return true;
    }
    const acceptedTypes = rule.properties['retention_rule:docTypes'];
    if (!acceptedTypes || !Array.isArray(acceptedTypes) || acceptedTypes.length === 0) {
      return true;
    }
    return acceptedTypes.indexOf(this.document.type) !== -1;
  }

  _ruleResultFormatter(doc) {
    let result = this._escapeHTML(doc.title);
    if (doc.properties && doc.properties['dc:description']) {
      result += `<span style="display:block;color:#9a9a9a;word-break:break-all;">${this._escapeHTML(
        doc.properties['dc:description'],
      )}</span>`;
    }
    return result;
  }

  _ruleSelectionFormatter(doc) {
    return this._escapeHTML(doc.title);
  }

  _escapeHTML(markup) {
    const replaceMap = {
      '\\': '&#92;',
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;',
      '/': '&#47;',
    };

    // Do not try to escape the markup if it's not a string
    if (typeof markup !== 'string') {
      return markup;
    }

    return String(markup).replace(/[&<>"'/\\]/g, (match) => replaceMap[match]);
  }
}
customElements.define(RetentionAttachRuleButton.is, RetentionAttachRuleButton);
