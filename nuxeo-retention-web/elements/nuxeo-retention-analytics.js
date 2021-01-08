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
import moment from '@nuxeo/moment';
import { mixinBehaviors } from '@polymer/polymer/lib/legacy/class.js';
import { html } from '@polymer/polymer/lib/utils/html-tag.js';
import { I18nBehavior } from '@nuxeo/nuxeo-ui-elements/nuxeo-i18n-behavior.js';
import { FormatBehavior } from '@nuxeo/nuxeo-ui-elements/nuxeo-format-behavior.js';
import '@nuxeo/nuxeo-dataviz-elements/nuxeo-repository-data.js';
import '@nuxeo/nuxeo-ui-elements/widgets/nuxeo-card.js';
import '@nuxeo/chart-elements/chart-bar.js';
import '@nuxeo/nuxeo-ui-elements/widgets/nuxeo-date-picker.js';

/**
`nuxeo-retention-analytics`
@group Nuxeo UI
@element nuxeo-retention-analytics
*/
class RetentionAnalytics extends mixinBehaviors([I18nBehavior, FormatBehavior], Nuxeo.Element) {
  static get template() {
    return html`
      <style include="nuxeo-styles">
        .infoCard {
          @apply --layout-horizontal;
          @apply --layout-justified;
          @apply --layout-baseline;
        }

        .rangeSelector {
          @apply --layout-horizontal;
          @apply --layout-baseline;
        }

        .dateLabel {
          margin: 0 8px;
        }

        .flex-layout {
          @apply --layout-wrap;
          @apply --layout-horizontal;
          @apply --layout-justified;
        }

        .flex-layout nuxeo-card {
          flex: 1 0;
          text-align: center;
          width: 32%;
          min-width: 30rem;
        }

        nuxeo-card .alignLeft {
          text-align: start;
        }

        nuxeo-data-table {
          height: 550px;
        }

        chart-bar,
        chart-pie {
          margin: 25px auto 0 auto;
          max-width: 100%;
          display: block;
          font-size: 0.8rem;
        }

        chart-bar {
          padding: 2em;
        }

        .mainRetentionInfo {
          color: #7e90a5;
          text-transform: uppercase;
          margin-inline-end: 8px;
        }

        .retentionCount {
          font-weight: bold;
        }

        .chartBar {
          text-align: center;
        }

        .retentionStatus {
          margin-right: 8px;
          margin-left: 8px;
        }
      </style>
      <nuxeo-page>
        <div slot="header">
          <span>[[i18n('retention.analytics')]]</span>
        </div>
        <!-- Number of documents under retention -->
        <nuxeo-repository-data
          start-date="[[_formatDate(startDate)]]"
          end-date="[[_extendEndDate(endDate)]]"
          date-field="dc:created"
          where='{"ecm:isRecord": "true"}'
          metrics="cardinality(ecm:uuid)"
          data="{{activeRetentionCount}}"
          index="[[index]]"
        >
        </nuxeo-repository-data>

        <!-- Number of Files under legal hold -->
        <nuxeo-repository-data
          start-date="[[_formatDate(startDate)]]"
          end-date="[[_extendEndDate(endDate)]]"
          date-field="dc:created"
          where='{"ecm:hasLegalHold": "true"}'
          metrics="cardinality(ecm:uuid)"
          data="{{legalHoldCount}}"
          index="[[index]]"
        >
        </nuxeo-repository-data>

        <nuxeo-card class="infoCard">
          <div class="rangeSelector">
            <span class="mainRetentionInfo"
              >[[i18n('retentionAnalytics.underActiveRetention')]]
              <span class="retentionCount">[[activeRetentionCount]]</span>
            </span>
            <span class="mainRetentionInfo"> | </span>
            <span class="mainRetentionInfo"
              >[[i18n('retentionAnalytics.legalHoldCount')]]
              <span class="retentionCount">[[legalHoldCount]]</span>
            </span>
          </div>
          <div class="rangeSelector">
            <label class="dateLabel" for="startDate">[[i18n('retentionAnalytics.from')]]</label>
            <nuxeo-date-picker id="startDate" value="{{startDate}}"></nuxeo-date-picker>
            <label class="dateLabel" for="endDate">[[i18n('retentionAnalytics.to')]]</label>
            <nuxeo-date-picker id="endDate" value="{{endDate}}"></nuxeo-date-picker>
          </div>
        </nuxeo-card>

        <div class="flex-layout">
          <!-- Retention by Document Type -->
          <nuxeo-repository-data
            start-date="[[_formatDate(startDate)]]"
            end-date="[[_extendEndDate(endDate)]]"
            date-field="dc:created"
            grouped-by="ecm:primaryType"
            where='{"ecm:isRecord": "true"}'
            group-limit="20"
            data="{{allDocCategoriesCount}}"
            index="[[index]]"
          >
          </nuxeo-repository-data>

          <nuxeo-card heading="[[i18n('retentionAnalytics.retentionByDocType')]]">
            <chart-pie
              values="[[_values(allDocCategoriesCount)]]"
              labels="[[_labels(allDocCategoriesCount)]]"
              options="{_piechartOptions}"
            >
            </chart-pie>
          </nuxeo-card>

          <!-- RETENTION STATUS -->
          <!-- No Retention -->
          <nuxeo-repository-data
            start-date="[[_formatDate(startDate)]]"
            end-date="[[_extendEndDate(endDate)]]"
            date-field="dc:created"
            metrics="cardinality(ecm:uuid)"
            where='{"ecm:isRecord": "false"}'
            data="{{noRetention}}"
            index="[[index]]"
          >
          </nuxeo-repository-data>

          <!-- Future Retentions -->
          <nuxeo-repository-data
            start-date="[[_formatDate(startDate)]]"
            end-date="[[_extendEndDate(endDate)]]"
            date-field="dc:created"
            where="[[_filterActive]]"
            metrics="cardinality(ecm:uuid)"
            data="{{activeRetention}}"
            index="[[index]]"
          >
          </nuxeo-repository-data>

          <!-- Expired Retentions -->
          <nuxeo-repository-data
            start-date="[[_formatDate(startDate)]]"
            end-date="[[_extendEndDate(endDate)]]"
            date-field="dc:created"
            where="[[_filterExpired]]"
            metrics="cardinality(ecm:uuid)"
            data="{{expiredRetention}}"
            index="[[index]]"
          >
          </nuxeo-repository-data>

          <!-- Interminate Retentions -->
          <nuxeo-repository-data
            start-date="[[_formatDate(startDate)]]"
            end-date="[[_extendEndDate(endDate)]]"
            date-field="dc:created"
            where="[[_filterInterminate]]"
            metrics="cardinality(ecm:uuid)"
            data="{{interminateRetention}}"
            index="[[index]]"
          >
          </nuxeo-repository-data>

          <nuxeo-card class="retentionStatus" heading="[[i18n('retentionAnalytics.retentionStatus')]]">
            <chart-pie
              values="[[_valuesRetentionStatus(noRetention, activeRetention, expiredRetention, interminateRetention)]]"
              labels="[[_labelsRetentionStatus]]"
              options="{_piechartOptions}"
            >
            </chart-pie>
          </nuxeo-card>

          <!-- LegalHold by Document Type -->
          <nuxeo-repository-data
            start-date="[[_formatDate(startDate)]]"
            end-date="[[_extendEndDate(endDate)]]"
            date-field="dc:created"
            grouped-by="ecm:primaryType"
            where='{"ecm:hasLegalHold": "true"}'
            group-limit="20"
            data="{{legalHoldByDocType}}"
            index="[[index]]"
          >
          </nuxeo-repository-data>

          <nuxeo-card heading="[[i18n('retentionAnalytics.legalHoldByDocType')]]">
            <chart-pie
              values="[[_values(legalHoldByDocType)]]"
              labels="[[_labels(legalHoldByDocType)]]"
              options="{_piechartOptions}"
            >
            </chart-pie>
          </nuxeo-card>
        </div>

        <!-- Expiration next 12 months -->
        <nuxeo-repository-data
          start-date="[[_formatDateNow()]]"
          end-date="[[_formatDateFutureInMonths(12)]]"
          with-date-intervals="month"
          date-field="ecm:retainUntil"
          data="{{expirationPerMonth}}"
          index="[[index]]"
        >
        </nuxeo-repository-data>

        <nuxeo-card class="chartBar" heading="[[i18n('retentionAnalytics.expirationNext12Months.heading')]]">
          <chart-bar
            labels="[[_futureExpirationLabels(expirationPerMonth)]]"
            values="[[_values(expirationPerMonth)]]"
            options='{ "legend": { "display": false }, "animation": false }'
          >
          </chart-bar>
        </nuxeo-card>
      </nuxeo-page>
    `;
  }

  static get is() {
    return 'nuxeo-retention-analytics';
  }

  static get properties() {
    return {
      index: {
        type: String,
        value: '_all',
      },
      visible: Boolean,
      startDate: {
        type: String,
        value: moment().subtract(1, 'year').format('YYYY-MM-DD'),
      },
      endDate: {
        type: String,
        value: moment().format('YYYY-MM-DD'),
      },
      _filterActive: {
        type: Array,
        value: [{ range: { 'ecm:retainUntil': { gte: moment().format('YYYY-MM-DD') } } }],
      },
      _filterExpired: {
        type: Array,
        value: [{ range: { 'record:retainUntil': { lte: moment().format('YYYY-MM-DD') } } }],
      },
      _filterInterminate: {
        type: Array,
        value: [{ range: { 'ecm:retainUntil': { gte: '9999-01-01T00:00:00.000Z' } } }],
      },
      _labelsRetentionStatus: {
        type: Array,
        value: ['No retention', 'Active retention', 'Expired retention', 'Interminate retention'],
      },
      _piechartOptions: {
        type: Object,
        value: { legend: { display: true, position: 'bottom', labels: { boxWidth: 12 } }, animation: false },
      },
    };
  }

  static get observers() {
    return ['_resetChartData(visible)'];
  }

  _resetChartData(visible) {
    if (visible) {
      const repositories = this.shadowRoot.querySelectorAll('nuxeo-repository-data');
      repositories.forEach((repository) => repository.fetch());
    }
  }

  _values(data) {
    return [data.map((entry) => entry.value)];
  }

  _labels(data) {
    return data.map((entry) => entry.key);
  }

  _valuesRetentionStatus(noRetention, activeRetention, expiredRetention, interminateRetention) {
    return [[noRetention, activeRetention, expiredRetention, interminateRetention]];
  }

  _extendEndDate(date) {
    return this._formatDate(moment(date).add(1, 'days').subtract(1, 'ms').toJSON());
  }

  _formatDateLabels(data, format) {
    return data.map((entry) => moment(entry.key).format(format));
  }

  _formatDateNow() {
    return moment().format('YYYY-MM-DD');
  }

  _formatDateFutureInMonths(count) {
    return moment().add(count, 'months').format('YYYY-MM-DD');
  }

  _futureExpirationLabels(data) {
    return this._labels(data).map((label) => moment(label).format('YYYY-MM'));
  }
}

customElements.define(RetentionAnalytics.is, RetentionAnalytics);
