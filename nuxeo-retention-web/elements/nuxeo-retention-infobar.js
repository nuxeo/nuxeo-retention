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
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
import { mixinBehaviors } from '@polymer/polymer/lib/legacy/class.js';
import '@polymer/iron-icon/iron-icon.js';
import '@polymer/iron-icons/iron-icons.js';
import '@nuxeo/nuxeo-elements/nuxeo-element.js';
import '@polymer/polymer/lib/elements/dom-if.js';
import { FiltersBehavior } from '@nuxeo/nuxeo-ui-elements/nuxeo-filters-behavior.js';
import { FormatBehavior } from '@nuxeo/nuxeo-ui-elements/nuxeo-format-behavior.js';

/**
`nuxeo-retention-infobar`
@group Nuxeo UI
@element nuxeo-retention-infobar
*/
class RetentionInfobar extends mixinBehaviors([FiltersBehavior, FormatBehavior], Nuxeo.Element) {
  static get template() {
    return html`
      <style include="nuxeo-styles iron-flex">
        .bar {
          @apply --layout-horizontal;
          @apply --layout-center;
          @apply --layout-justified;
          padding: 8px;
          margin-bottom: 16px;
          box-shadow: 0 3px 5px rgba(0, 0, 0, 0.04);
          background-color: var(--nuxeo-box);
        }

        .flexibleRetention {
          margin-right: 3px;
        }

        .item {
          @apply --layout-horizontal;
          @apply --layout-center;
          @apply --layout-flex;
        }

        iron-icon {
          margin: 0 0.5em;
          width: 1.5em;
        }
      </style>

      <nuxeo-operation id="unretainOp" op="Document.UnattachRetentionRule" input="[[document]]"> </nuxeo-operation>

      <!-- Record -->
      <dom-if if="[[_isAvailable(document)]]">
        <template>
          <div id="flexibleRetentionInfoBar" class="bar record">
            <div class="item">
              <iron-icon icon="nuxeo:retain"></iron-icon>
              <span id="flexibleRetention">[[i18n('retention.action.unretain.description')]]</span>
            </div>
            <paper-button class="primary" on-tap="_unretain" noink>[[i18n('retention.action.unretain')]]</paper-button>
          </div>
        </template>
      </dom-if>
    `;
  }

  static get is() {
    return 'nuxeo-retention-infobar';
  }

  static get properties() {
    return {
      /**
       * Input document.
       */
      document: Object,
    };
  }

  _isAvailable(document) {
    return (
      document &&
      document.isFlexibleRecord &&
      this.hasFacet(document, 'Record') &&
      this.hasPermission(document, 'UnsetRetention')
    );
  }

  _unretain() {
    if (!window.confirm(this.i18n('retention.action.unretain.confirm'))) {
      return;
    }
    this.$.unretainOp.execute().then(() => {
      this.dispatchEvent(
        new CustomEvent('document-updated', {
          composed: true,
          bubbles: true,
        }),
      );
    });
  }
}

customElements.define(RetentionInfobar.is, RetentionInfobar);
