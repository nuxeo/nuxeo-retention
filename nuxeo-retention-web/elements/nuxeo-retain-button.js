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
import '@nuxeo/nuxeo-ui-elements/widgets/nuxeo-date-picker.js';
import '@nuxeo/nuxeo-ui-elements/widgets/nuxeo-tooltip.js';
import '@polymer/paper-button/paper-button.js';
import '@polymer/paper-dialog-scrollable/paper-dialog-scrollable.js';
import '@polymer/paper-icon-button/paper-icon-button.js';

/**
`nuxeo-retain-button`
@group Nuxeo UI
@element nuxeo-retain-button
*/

class RetentionRetainButton extends mixinBehaviors([FiltersBehavior, FormatBehavior], Nuxeo.Element) {
  static get template() {
    return html`
      <style include="nuxeo-action-button-styles">
        /* Fix known stacking issue in iOS (NXP-24600)
          https://github.com/PolymerElements/paper-dialog-scrollable/issues/72 */
        paper-dialog-scrollable {
          --paper-dialog-scrollable: {
            -webkit-overflow-scrolling: auto;
          }
        }
      </style>

      <nuxeo-operation id="retainOp" op="Document.Retain" input="[[document]]"> </nuxeo-operation>

      <dom-if if="[[_isAvailable(document)]]">
        <template>
          <div class="action" on-click="_toggleDialog">
            <paper-icon-button icon="[[icon]]" noink=""></paper-icon-button>
            <span class="label" hidden$="[[!showLabel]]">[[_label]]</span>
            <nuxeo-tooltip>[[_label]]</nuxeo-tooltip>
          </div>
        </template>
      </dom-if>

      <nuxeo-dialog id="dialog" with-backdrop="">
        <h2>[[i18n('retention.action.dialog.heading')]]</h2>
        <paper-dialog-scrollable>
          <nuxeo-date-picker
            id="picker"
            label="[[i18n('retention.action.until')]]"
            value="{{until}}"
            required$="[[document.retainUntil]]"
            min="[[_computeMinDate(document)]]"
          >
          </nuxeo-date-picker>
        </paper-dialog-scrollable>
        <div class="buttons">
          <paper-button name="cancel" dialog-dismiss="" class="secondary">[[i18n('command.close')]]</paper-button>
          <paper-button name="add" class="primary" on-tap="_retain" disabled$="[[!_isValid(document, until)]]">
            [[i18n('retention.action.retain')]]
          </paper-button>
        </div>
      </nuxeo-dialog>
    `;
  }

  static get is() {
    return 'nuxeo-retain-button';
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
        value: 'nuxeo:retain',
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

      /**
       * Retention expiration date.
       */
      until: Date,
    };
  }

  _isAvailable(document) {
    return this.canSetRetention(document);
  }

  _computeLabel() {
    return this.i18n('retention.action.retain');
  }

  _toggleDialog() {
    this.$.dialog.toggle();
  }

  _retain() {
    this.$.retainOp.params = { until: this.until };
    this.$.retainOp.execute().then(() => {
      this.dispatchEvent(
        new CustomEvent('document-updated', {
          composed: true,
          bubbles: true,
        }),
      );
      this._toggleDialog();
    });
  }

  _computeMinDate() {
    this.set('until', undefined);
    if (this.document.retainUntil && !this.isRetentionDateIndeterminate(this.document)) {
      this.set('until', this.document.retainUntil);
      return this.formatDate(this.document.retainUntil, 'YYYY-MM-DD');
    }
    // Min date is tomorrow
    const tomorrow = moment().add(1, 'days');
    return this.formatDate(tomorrow.toJSON(), 'YYYY-MM-DD');
  }

  _isValid() {
    return this.document && this.document.retainUntil ? !!this.until : true;
  }
}
customElements.define(RetentionRetainButton.is, RetentionRetainButton);
