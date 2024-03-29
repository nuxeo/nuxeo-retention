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

// eslint-disable-next-line import/prefer-default-export
export const Nuxeo = window.Nuxeo || {};

let fetcher;

function _fetchDateFields(onlySchemas) {
  if (onlySchemas.size > 0) {
    let schemaFetcher = document.createElement('nuxeo-resource');
    document.body.appendChild(schemaFetcher);
    schemaFetcher.path = 'config/schemas';
    return schemaFetcher
      .get()
      .then((res) => {
        const dateFields = [];
        if (res) {
          res.forEach((schema) => {
            if (onlySchemas.has(schema.name)) {
              Object.keys(schema.fields).forEach((fieldName) => {
                if (schema.fields[fieldName] === 'date') {
                  dateFields.push(`${schema['@prefix']}:${fieldName}`);
                }
              });
            }
          });
        }
        return dateFields;
      })
      .finally(() => {
        document.body.removeChild(schemaFetcher);
        schemaFetcher = null;
      });
  }
  return null;
}

function _fetchDocTypes() {
  let typeFetcher = document.createElement('nuxeo-resource');
  document.body.appendChild(typeFetcher);
  typeFetcher.path = 'config/types';
  return typeFetcher
    .get()
    .then(async (res) => {
      let docTypes;
      let dateFields;
      if (res && res.doctypes) {
        const schemas = new Set();
        docTypes = [];
        Object.keys(res.doctypes).forEach((type) => {
          if (
            res.doctypes[type].schemas.indexOf('file') !== -1 &&
            res.doctypes[type].facets.indexOf('Folderish') === -1 &&
            res.doctypes[type].facets.indexOf('HiddenInNavigation') === -1 &&
            res.doctypes[type].facets.indexOf('SystemDocument') === -1
          ) {
            docTypes.push({
              id: type,
              text: type,
            });
            res.doctypes[type].schemas.forEach((schema) => {
              schemas.add(schema);
            });
          }
        });
        dateFields = await _fetchDateFields(schemas);
      }
      return { docTypes, dateFields };
    })
    .finally(() => {
      document.body.removeChild(typeFetcher);
      typeFetcher = null;
    });
}

/**
 * `Nuxeo.RetentionBehavior` provides a set of helpers to format values.
 *
 * @polymerBehavior Nuxeo.RetentionBehavior
 */
Nuxeo.RetentionBehavior = {
  properties: {
    /**
     * Document types on which retention rules can be attached.
     * i.e. types with `file` schema and no `Folderish`, `HiddenInNavifation`, `SystemDocument` facet.
     */
    docTypes: Array,

    /**
     * Schema fields of type `date`.
     */
    dateFields: Array,
  },

  ready() {
    if (!fetcher) {
      fetcher = _fetchDocTypes();
    }
    fetcher.then(({ docTypes, dateFields }) => {
      this.docTypes = docTypes;
      this.dateFields = dateFields;
    });
  },

  _isAuto() {
    return (
      this.document &&
      this.document.properties &&
      this.document.properties['retention_rule:applicationPolicy'] === 'auto'
    );
  },

  _isImmediate() {
    return (
      this.document &&
      this.document.properties &&
      this.document.properties['retention_def:startingPointPolicy'] === 'immediate'
    );
  },

  _isEventBased() {
    return (
      this.document &&
      this.document.properties &&
      this.document.properties['retention_def:startingPointPolicy'] === 'event_based'
    );
  },

  _isMetadataBased() {
    return (
      this.document &&
      this.document.properties &&
      this.document.properties['retention_def:startingPointPolicy'] === 'metadata_based'
    );
  },

  _isAfterDelay() {
    return (
      this.document &&
      this.document.properties &&
      this.document.properties['retention_def:startingPointPolicy'] === 'after_delay'
    );
  },

  _isManual() {
    return (
      this.document &&
      this.document.properties &&
      this.document.properties['retention_rule:applicationPolicy'] === 'manual'
    );
  },

  _computeApplicationPolicyLabel() {
    return this.document
      ? this.i18n(
          `retention.rule.label.applicationPolicy.${this.document.properties['retention_rule:applicationPolicy']}`,
        )
      : '';
  },

  _computeStartPolicyLabel() {
    return this.document
      ? this.i18n(`retention.rule.label.startPolicy.${this.document.properties['retention_def:startingPointPolicy']}`)
      : '';
  },
};
