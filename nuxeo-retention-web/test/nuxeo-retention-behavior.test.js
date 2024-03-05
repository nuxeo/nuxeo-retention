/**
@license
©2023 Hyland Software, Inc. and its affiliates. All rights reserved. 
All Hyland product names are registered or unregistered trademarks of Hyland Software, Inc. or its affiliates.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use retentionBehaviorInstance file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import sinon from 'sinon';
import { expect } from 'chai';
import { Nuxeo } from '../elements/nuxeo-retention-behavior.js';

suite('nuxeo-retention-behavior', () => {
  const document = {
    'entity-type': 'document',
    contextParameters: {
      attachEl: {
        entries: [
          {
            path: '/default-domain',
            title: 'Domain',
            type: 'Domain',
            uid: '1',
          },
          {
            path: '/default-domain/workspaces',
            title: 'Workspaces',
            type: 'WorkspaceRoot',
            uid: '2',
          },
          {
            path: '/default-domain/workspaces/my workspace',
            title: 'my workspace',
            type: 'Workspace',
            uid: '3',
          },
          {
            path: '/default-domain/workspaces/my workspace/folder 1',
            title: 'folder 1',
            type: 'Folder',
            uid: '4',
          },
          {
            path: '/default-domain/workspaces/my workspace/folder 1/folder 2',
            title: 'folder 2',
            type: 'Folder',
            uid: '5',
          },
          {
            path: '/default-domain/workspaces/my workspace/folder 1/folder 2/folder 3',
            title: 'folder 3',
            type: 'Folder',
            uid: '6',
          },
          {
            path: '/default-domain/workspaces/my workspace/folder 1/folder 2/folder 3/my file',
            title: 'my file',
            type: 'File',
            uid: '7',
          },
        ],
      },
    },
    path: '/default-domain/workspaces/my workspace/folder 1/folder 2/folder 3/my file',
    title: 'my file',
    type: 'File',
    uid: '7',
    properties: {},
  };
  let sandbox;
  let retentionBehaviorInstance;
  const originalRetentionBehaviorObj = Nuxeo.RetentionBehavior;

  setup(async () => {
    retentionBehaviorInstance = Object.create(Nuxeo.RetentionBehavior);
    sandbox = sinon.createSandbox();
  });

  teardown(() => {
    Nuxeo.RetentionBehavior = originalRetentionBehaviorObj;
    sandbox.restore();
  });

  suite('test _isAuto', () => {
    test('Should return true if document is available & document.properties is available & document.properties["retention_rule:applicationPolicy"] = auto', async () => {
      retentionBehaviorInstance.document = document;
      retentionBehaviorInstance.document.properties['retention_rule:applicationPolicy'] = 'auto';
      expect(retentionBehaviorInstance._isAuto()).equal(true);
    });

    test('Should return false if document is available & document.properties is available but document.properties["retention_rule:applicationPolicy"] != auto', async () => {
      retentionBehaviorInstance.document = document;
      retentionBehaviorInstance.document.properties['retention_rule:applicationPolicy'] = 'custom';
      expect(retentionBehaviorInstance._isAuto()).equal(false);
    });

    test('Should return false if document is available but document.properties is not available', async () => {
      retentionBehaviorInstance.document = document;
      expect(retentionBehaviorInstance._isAuto()).equal(false);
    });

    test('Should return false if document is not available', async () => {
      retentionBehaviorInstance.document = {};
      expect(retentionBehaviorInstance._isAuto()).equal(undefined);
    });
  });

  suite('test _isImmediate', () => {
    test('Should return true if document is available & document.properties is available & document.properties["retention_def:startingPointPolicy"] = immediate', async () => {
      retentionBehaviorInstance.document = document;
      retentionBehaviorInstance.document.properties['retention_def:startingPointPolicy'] = 'immediate';
      expect(retentionBehaviorInstance._isImmediate()).equal(true);
    });

    test('Should return false if document is available & document.properties is available but document.properties["retention_def:startingPointPolicy"] != immediate', async () => {
      retentionBehaviorInstance.document = document;
      retentionBehaviorInstance.document.properties['retention_def:startingPointPolicy'] = 'custom';
      expect(retentionBehaviorInstance._isImmediate()).equal(false);
    });

    test('Should return false if document is available but document.properties is not available', async () => {
      retentionBehaviorInstance.document = document;
      expect(retentionBehaviorInstance._isImmediate()).equal(false);
    });

    test('Should return false if document is not available', async () => {
      retentionBehaviorInstance.document = {};
      expect(retentionBehaviorInstance._isImmediate()).equal(undefined);
    });
  });

  suite('test _isEventBased', () => {
    test('Should return true if document is available & document.properties is available & document.properties["retention_def:startingPointPolicy"] = event_based', async () => {
      retentionBehaviorInstance.document = document;
      retentionBehaviorInstance.document.properties['retention_def:startingPointPolicy'] = 'event_based';
      expect(retentionBehaviorInstance._isEventBased()).equal(true);
    });

    test('Should return false if document is available & document.properties is available but document.properties["retention_def:startingPointPolicy"] != event_based', async () => {
      retentionBehaviorInstance.document = document;
      retentionBehaviorInstance.document.properties['retention_def:startingPointPolicy'] = 'custom';
      expect(retentionBehaviorInstance._isEventBased()).equal(false);
    });

    test('Should return false if document is available but document.properties is not available', async () => {
      retentionBehaviorInstance.document = document;
      expect(retentionBehaviorInstance._isEventBased()).equal(false);
    });

    test('Should return false if document is not available', async () => {
      retentionBehaviorInstance.document = {};
      expect(retentionBehaviorInstance._isEventBased()).equal(undefined);
    });
  });

  suite('test _isMetadataBased', () => {
    test('Should return true if document is available & document.properties is available & document.properties["retention_def:startingPointPolicy"] = metadata_based', async () => {
      retentionBehaviorInstance.document = document;
      retentionBehaviorInstance.document.properties['retention_def:startingPointPolicy'] = 'metadata_based';
      expect(retentionBehaviorInstance._isMetadataBased()).equal(true);
    });

    test('Should return false if document is available & document.properties is available but document.properties["retention_def:startingPointPolicy"] != metadata_based', async () => {
      retentionBehaviorInstance.document = document;
      retentionBehaviorInstance.document.properties['retention_def:startingPointPolicy'] = 'custom';
      expect(retentionBehaviorInstance._isMetadataBased()).equal(false);
    });

    test('Should return false if document is available but document.properties is not available', async () => {
      retentionBehaviorInstance.document = document;
      expect(retentionBehaviorInstance._isMetadataBased()).equal(false);
    });

    test('Should return false if document is not available', async () => {
      retentionBehaviorInstance.document = {};
      expect(retentionBehaviorInstance._isMetadataBased()).equal(undefined);
    });
  });

  suite('test _isAfterDelay', () => {
    test('Should return true if document is available & document.properties is available & document.properties["retention_def:startingPointPolicy"] = after_delay', async () => {
      retentionBehaviorInstance.document = document;
      retentionBehaviorInstance.document.properties['retention_def:startingPointPolicy'] = 'after_delay';
      expect(retentionBehaviorInstance._isAfterDelay()).equal(true);
    });

    test('Should return false if document is available & document.properties is available but document.properties["retention_def:startingPointPolicy"] != after_delay', async () => {
      retentionBehaviorInstance.document = document;
      retentionBehaviorInstance.document.properties['retention_def:startingPointPolicy'] = 'custom';
      expect(retentionBehaviorInstance._isAfterDelay()).equal(false);
    });

    test('Should return false if document is available but document.properties is not available', async () => {
      retentionBehaviorInstance.document = document;
      expect(retentionBehaviorInstance._isAfterDelay()).equal(false);
    });

    test('Should return false if document is not available', async () => {
      retentionBehaviorInstance.document = {};
      expect(retentionBehaviorInstance._isAfterDelay()).equal(undefined);
    });
  });

  suite('test _isManual', () => {
    test('Should return true if document is available & document.properties is available & document.properties["retention_rule:applicationPolicy"] = manual', async () => {
      retentionBehaviorInstance.document = document;
      retentionBehaviorInstance.document.properties['retention_rule:applicationPolicy'] = 'manual';
      expect(retentionBehaviorInstance._isManual()).equal(true);
    });

    test('Should return false if document is available & document.properties is available but document.properties["retention_rule:applicationPolicy"] != manual', async () => {
      retentionBehaviorInstance.document = document;
      retentionBehaviorInstance.document.properties['retention_rule:applicationPolicy'] = 'custom';
      expect(retentionBehaviorInstance._isManual()).equal(false);
    });

    test('Should return false if document is available but document.properties is not available', async () => {
      retentionBehaviorInstance.document = document;
      expect(retentionBehaviorInstance._isManual()).equal(false);
    });

    test('Should return false if document is not available', async () => {
      retentionBehaviorInstance.document = {};
      expect(retentionBehaviorInstance._isManual()).equal(undefined);
    });
  });

  suite('test _computeApplicationPolicyLabel', () => {
    test('Should return empty string if document is not available', async () => {
      expect(retentionBehaviorInstance._computeApplicationPolicyLabel()).equal('');
    });
  });

  suite('test _computeStartPolicyLabel', () => {
    test('Should return empty string if document is not available', async () => {
      expect(retentionBehaviorInstance._computeStartPolicyLabel()).equal('');
    });
  });

  suite('test ready', () => {
    test('should set docTypes and dateFields when fetcher resolves', async () => {
      const resObj = Promise.resolve({
        docTypes: [
          {
            doc: {
              schemas: ['file', 'folder', 'picture'],
              facets: ['record'],
            },
          },
        ],
      });

      sandbox.stub(window.document, 'createElement').returns({ get: sandbox.stub().resolves(resObj) });
      sandbox.stub(window.document.body, 'appendChild');
      sandbox.stub(window.document.body, 'removeChild');
      await retentionBehaviorInstance.ready();
      expect(retentionBehaviorInstance.properties.docTypes).to.be.an.instanceof(Function);
      expect(retentionBehaviorInstance.properties.dateFields).to.be.an.instanceof(Function);
    });
  });
});
