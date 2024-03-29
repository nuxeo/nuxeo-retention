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
import { I18nBehavior } from '@nuxeo/nuxeo-ui-elements/nuxeo-i18n-behavior.js';
import '@nuxeo/nuxeo-elements/nuxeo-audit-page-provider.js';
import '@nuxeo/nuxeo-elements/nuxeo-operation.js';
import '@nuxeo/nuxeo-ui-elements/nuxeo-data-table/iron-data-table.js';
import '@nuxeo/nuxeo-ui-elements/nuxeo-data-table/data-table-column.js';
import '@nuxeo/nuxeo-ui-elements/widgets/nuxeo-card.js';
import '@nuxeo/nuxeo-ui-elements/widgets/nuxeo-date.js';
import '@nuxeo/nuxeo-ui-elements/widgets/nuxeo-date-picker.js';
import '@nuxeo/nuxeo-ui-elements/widgets/nuxeo-directory-suggestion.js';
import '@nuxeo/nuxeo-ui-elements/widgets/nuxeo-input.js';
import '@nuxeo/nuxeo-ui-elements/widgets/nuxeo-user-tag.js';
import '@polymer/paper-button/paper-button.js';

/**
`nuxeo-retention-events`
@group Nuxeo UI
@element nuxeo-retention-events
*/
class RetentionEvents extends mixinBehaviors([I18nBehavior], Nuxeo.Element) {
  static get template() {
    return html`
      <style include="nuxeo-styles">
        #heading {
          @apply --layout-horizontal;
          @apply --layout-end-justified;
        }

        #table {
          height: 50vh;
        }

        nuxeo-date-picker {
          padding: 0 16px;
        }
      </style>
      <nuxeo-page>
        <div slot="header">
          <span class="flex">[[i18n('retention.events')]]</span>
        </div>
        <nuxeo-card heading="[[i18n('retention.events.fire')]]">
          <nuxeo-operation id="op" op="Retention.FireEvent" loading="{{firingEvent}}"> </nuxeo-operation>
          <nuxeo-directory-suggestion
            name="event"
            role="widget"
            value="{{_event}}"
            label="[[i18n('retention.rule.label.startPolicy.eventBased.event.description')]]"
            required
            query-results-filter="[[_filterEvents]]"
            directory-name="RetentionEvent"
            min-chars="0"
          >
          </nuxeo-directory-suggestion>
          <nuxeo-input name="eventInput" value="{{_eventInput}}" label="[[i18n('retention.events.input')]]">
          </nuxeo-input>
          <div class="buttons">
            <paper-button name="fire" class="primary" on-tap="_fire" disabled$="[[!_canFire(_event, firingEvent)]]">
              [[i18n('retention.events.fire')]]
            </paper-button>
          </div>
        </nuxeo-card>

        <nuxeo-card heading="[[i18n('retention.events.history')]]">
          <nuxeo-audit-page-provider
            id="provider"
            page-size="40"
            params='{"eventCategory":"Retention"}'
          ></nuxeo-audit-page-provider>
          <div id="heading">
            <template is="dom-if" if="[[visible]]">
              <nuxeo-date-picker role="widget" label="[[i18n('documentHistory.filter.after')]]" value="{{startDate}}">
              </nuxeo-date-picker>
              <nuxeo-date-picker role="widget" label="[[i18n('documentHistory.filter.before')]]" value="{{endDate}}">
              </nuxeo-date-picker>
            </template>
          </div>

          <nuxeo-data-table
            id="table"
            paginable
            nx-provider="provider"
            empty-label="[[i18n('retention.events.empty')]]"
          >
            <nuxeo-data-table-column
              name="[[i18n('retention.rule.label.startPolicy.eventBased.event.description')]]"
              sort-by="eventId"
            >
              <template>[[item.eventId]]</template>
            </nuxeo-data-table-column>
            <nuxeo-data-table-column name="[[i18n('documentHistory.date')]]" sort-by="eventDate">
              <template><nuxeo-date datetime="[[item.eventDate]]"></nuxeo-date></template>
            </nuxeo-data-table-column>
            <nuxeo-data-table-column name="[[i18n('documentHistory.username')]]" sort-by="principalName">
              <template><nuxeo-user-tag user="[[item.principalName]]"></nuxeo-user-tag></template>
            </nuxeo-data-table-column>
            <nuxeo-data-table-column name="[[i18n('retention.events.input')]]">
              <template> [[item.comment]] </template>
            </nuxeo-data-table-column>
          </nuxeo-data-table>
        </nuxeo-card>
      </nuxeo-page>
    `;
  }

  static get is() {
    return 'nuxeo-retention-events';
  }

  static get properties() {
    return {
      visible: {
        type: Boolean,
        observer: '_refreshHistory',
      },
      _event: {
        type: String,
        value: '',
      },
      _eventInput: String,
      startDate: {
        type: String,
        notify: true,
        observer: '_observeStartDate',
      },
      endDate: {
        type: String,
        notify: true,
        observer: '_observeEndDate',
      },
    };
  }

  _observeStartDate() {
    if (this.startDate && this.startDate.length > 0) {
      this.$.provider.params.startDate = this.startDate;
      if (this.endDate && this.endDate.length > 0) {
        const start = Date.parse(this.startDate);
        const end = Date.parse(this.endDate);
        if (start > end) {
          this.endDate = moment(start).add(7, 'day').format('YYYY-MM-DD');
        }
      }
      this._refreshHistory();
    } else if (this.$.provider.params.startDate) {
      delete this.$.provider.params.startDate;
      this._refreshHistory();
    }
  }

  _observeEndDate() {
    if (this.endDate && this.endDate.length > 0) {
      this.$.provider.params.endDate = this.endDate;
      if (this.startDate && this.startDate.length > 0) {
        const start = Date.parse(this.startDate);
        const end = Date.parse(this.endDate);
        if (start > end) {
          this.startDate = moment(end).subtract(7, 'day').format('YYYY-MM-DD');
        }
      }
      this._refreshHistory();
    } else if (this.$.provider.params.endDate) {
      delete this.$.provider.params.endDate;
      this._refreshHistory();
    }
  }

  _refreshHistory(delay) {
    if (this.visible) {
      this.$.provider.page = 1;
      this.$.table.reset();
      this.$.table.loading = true;
      this.$.table.emptyLabel = this.i18n('label.loading');
      window.setTimeout(() => {
        this.$.table.fetch().then(() => {
          this.$.table.emptyLabel = this.i18n('retention.events.empty');
        });
      }, delay);
    }
  }

  _fire() {
    this.$.op.params = { name: this._event };
    this.$.op.input = this._eventInput;
    this.$.op.execute().then(() => {
      this.dispatchEvent(
        new CustomEvent('notify', {
          composed: true,
          bubbles: true,
          detail: { message: this.i18n('retention.events.fired.success') },
        }),
      );
      this._event = null;
      this._eventInput = null;
      // Audit is async, let's give it a little time to index
      this._refreshHistory(1000);
    });
  }

  _canFire() {
    return !this.firingEvent && !!this._event;
  }

  _filterEvents(evt) {
    return evt && evt.id && evt.id.indexOf('Retention.') === 0;
  }
}
customElements.define(RetentionEvents.is, RetentionEvents);
