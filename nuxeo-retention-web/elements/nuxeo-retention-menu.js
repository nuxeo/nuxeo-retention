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
import { I18nBehavior } from '@nuxeo/nuxeo-ui-elements/nuxeo-i18n-behavior.js';
import { RoutingBehavior } from '@nuxeo/nuxeo-ui-elements/nuxeo-routing-behavior.js';

/**
`nuxeo-retention-menu`
@group Nuxeo UI
@element nuxeo-retention-menu
*/
class RetentionMenu extends mixinBehaviors([FiltersBehavior, I18nBehavior, RoutingBehavior], Nuxeo.Element) {
  static get template() {
    return html`
      <style include="nuxeo-styles">
        nuxeo-menu-item:hover {
          @apply --nuxeo-block-hover;
        }

        nuxeo-menu-item:focus {
          @apply --nuxeo-block-selected;
        }

        nuxeo-menu-item {
          @apply --nuxeo-sidebar-item-theme;
          --nuxeo-menu-item-link {
            @apply --nuxeo-sidebar-item-link;
          }
        }
      </style>
      <div name="retention">
        <div class="header">[[i18n('retention.menu')]]</div>
        <nuxeo-menu-item label="retention.rules" name="rules" link="[[urlFor('document', '/RetentionRules')]]">
        </nuxeo-menu-item>
        <nuxeo-menu-item label="retention.search" name="search" route="page:retentionSearch"> </nuxeo-menu-item>
        <nuxeo-menu-item label="retention.events" name="events" route="page:retentionEvents"> </nuxeo-menu-item>
      </div>
    `;
  }

  static get is() {
    return 'nuxeo-retention-menu';
  }

  static get properties() {
    return {};
  }
}

customElements.define(RetentionMenu.is, RetentionMenu);
