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
import '@nuxeo/nuxeo-ui-elements/widgets/nuxeo-textarea.js';
import '@nuxeo/nuxeo-ui-elements/widgets/nuxeo-tooltip.js';
import '@polymer/paper-dialog-scrollable/paper-dialog-scrollable.js';
import '@polymer/paper-icon-button/paper-icon-button.js';
import '@polymer/paper-button/paper-button.js';

/**
`nuxeo-hold-toggle-button`
@group Nuxeo UI
@element nuxeo-hold-toggle-button
*/
class RetentionHoldToggleButton extends mixinBehaviors([FiltersBehavior, FormatBehavior], Nuxeo.Element) {
  static get template() {
    return html`
      <style include="nuxeo-styles nuxeo-action-button-styles">
        :host([hold]) paper-icon-button {
          color: var(--icon-toggle-outline-color, var(--nuxeo-action-color-activated));
        }
      </style>

      <nuxeo-operation id="waitEs" op="Elasticsearch.WaitForIndexing" params='{ "timeoutSecond": 5, "refresh": true }'>
      </nuxeo-operation>
      <nuxeo-operation id="opHold" on-poll-start="_onHoldPollStart" on-response="_onHoldResponse"> </nuxeo-operation>
      <nuxeo-operation id="opUnhold" on-poll-start="_onUnholdPollStart" on-response="_onUnholdResponse">
      </nuxeo-operation>

      <dom-if if="[[_isAvailable(provider, document)]]">
        <template>
          <div class="action" on-click="_toggle">
            <paper-icon-button icon="[[icon]]" noink="" on-clock=""></paper-icon-button>
            <span class="label" hidden$="[[!showLabel]]">[[_label]]</span>
            <nuxeo-tooltip>[[tooltip]]</nuxeo-tooltip>
          </div>
        </template>
      </dom-if>

      <nuxeo-dialog id="dialog" with-backdrop="" on-iron-overlay-closed="_resetPopup" no-auto-focus="">
        <h2>[[i18n('retention.holdToggleButton.label.heading')]]</h2>
        <paper-dialog-scrollable>
          <nuxeo-textarea
            name="description"
            label="[[i18n('retention.holdToggleButton.label.description')]]"
            value="{{description}}"
          ></nuxeo-textarea>
        </paper-dialog-scrollable>
        <div class="buttons">
          <paper-button dialog-dismiss="" class="secondary">[[i18n('command.close')]]</paper-button>
          <paper-button name="hold" class="primary" on-tap="_hold"> [[_label]] </paper-button>
        </div>
      </nuxeo-dialog>
    `;
  }

  static get is() {
    return 'nuxeo-hold-toggle-button';
  }

  static get properties() {
    return {
      /**
       * Input document.
       */
      document: {
        type: Object,
        observer: '_documentChanged',
      },

      /**
       * Icon to use (iconset_name:icon_name).
       */
      icon: {
        type: String,
        computed: '_computeIcon(hold)',
      },

      /**
       * Hold state.
       */
      hold: {
        type: Boolean,
        notify: true,
        reflectToAttribute: true,
        value: false,
      },

      /**
       * The translated label to be displayed by the action.
       */
      tooltip: {
        type: String,
        notify: true,
        computed: '_computeTooltip(hold, i18n)',
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
        computed: '_computeLabel(hold, i18n)',
      },

      /**
       * Page provider from which results are to be attached.
       */
      provider: {
        type: Object,
      },

      /**
       * Description to be set along with the hold.
       */
      description: String,
    };
  }

  _isAvailable() {
    return this.provider || this.canSetLegalHold(this.document);
  }

  _hold() {
    const { isFlexibleRecord, isUnderRetentionOrLegalHold } = this.document;
    const response =
      isFlexibleRecord && isUnderRetentionOrLegalHold
        ? window.confirm(this.i18n('retention.holdToggleButton.confirm.hold'))
        : true;
    if (response) {
      if (this.provider) {
        this.$.opHold.op = 'Bulk.RunAction';
        this.$.opHold.input = this.provider;
        this.$.opHold.async = true;
        this.$.opHold.params = {
          action: 'holdDocumentsAction',
        };
        if (this.description) {
          this.$.opHold.params.parameters = JSON.stringify({ description: this.description });
        }
        this.$.opHold.execute().then(() => {
          this._toggleDialog();
        });
      } else {
        this.$.opHold.op = 'Document.Hold';
        this.$.opHold.input = this.document;
        this.$.opHold.async = false;
        if (this.description) {
          this.$.opHold.params = { description: this.description };
        }
        this.$.opHold.execute().then(() => {
          this._toggleDialog();
          this.dispatchEvent(
            new CustomEvent('document-updated', {
              composed: true,
              bubbles: true,
            }),
          );
        });
      }
    }
  }

  _unhold() {
    if (this.provider) {
      this.$.opUnhold.op = 'Bulk.RunAction';
      this.$.opUnhold.input = this.provider;
      this.$.opUnhold.async = true;
      this.$.opUnhold.params = {
        action: 'unholdDocumentsAction',
      };
      this.$.opUnhold.execute();
    } else {
      this.$.opUnhold.op = 'Document.Unhold';
      this.$.opUnhold.input = this.document;
      this.$.opUnhold.async = false;
      this.$.opUnhold.params = {};
      this.$.opUnhold.execute().then(() => {
        this.dispatchEvent(
          new CustomEvent('document-updated', {
            composed: true,
            bubbles: true,
          }),
        );
      });
    }
  }

  _toggle() {
    if (!this.hold) {
      this._toggleDialog();
    } else {
      this._unhold();
    }
  }

  _toggleDialog() {
    this._resetPopup();
    this.$.dialog.toggle();
  }

  _resetPopup() {
    this.set('description', null);
  }

  _computeTooltip() {
    return this.i18n(`retention.holdToggleButton.tooltip.${this.hold ? 'unhold' : 'hold'}`);
  }

  _computeLabel() {
    return this.i18n(`retention.holdToggleButton.tooltip.${this.hold ? 'unhold' : 'hold'}`);
  }

  _computeIcon() {
    return this.hold ? 'nuxeo:hold' : 'nuxeo:unhold';
  }

  _documentChanged() {
    this.hold = !!(this.document && this.document.hasLegalHold);
  }

  _onHoldPollStart() {
    this.dispatchEvent(
      new CustomEvent('notify', {
        composed: true,
        bubbles: true,
        detail: { message: this.i18n('retention.holdToggleButton.bulk.hold.poll') },
      }),
    );
  }

  _onHoldResponse() {
    this.$.waitEs.execute().then(() => {
      this.dispatchEvent(
        new CustomEvent('notify', {
          composed: true,
          bubbles: true,
          detail: { message: this.i18n('retention.holdToggleButton.bulk.hold') },
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

  _onUnholdPollStart() {
    this.dispatchEvent(
      new CustomEvent('notify', {
        composed: true,
        bubbles: true,
        detail: { message: this.i18n('retention.holdToggleButton.bulk.unhold.poll') },
      }),
    );
  }

  _onUnholdResponse() {
    this.$.waitEs.execute().then(() => {
      this.dispatchEvent(
        new CustomEvent('notify', {
          composed: true,
          bubbles: true,
          detail: { message: this.i18n('retention.holdToggleButton.bulk.unhold') },
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
}
customElements.define(RetentionHoldToggleButton.is, RetentionHoldToggleButton);
