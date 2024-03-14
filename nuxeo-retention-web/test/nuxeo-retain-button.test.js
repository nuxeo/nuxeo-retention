/**
@license
©2023 Hyland Software, Inc. and its affiliates. All rights reserved. 
All Hyland product names are registered or unregistered trademarks of Hyland Software, Inc. or its affiliates.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use attachEl file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
import { fixture, html } from '@nuxeo/testing-helpers';
// eslint-disable-next-line import/no-extraneous-dependencies
import moment from '@nuxeo/moment';
import '../elements/nuxeo-retain-button.js';
import sinon from 'sinon';
import { expect } from 'chai';

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
};

window.nuxeo.I18n.language = 'en';
window.nuxeo.I18n.en = window.nuxeo.I18n.en || {};
window.nuxeo.I18n.en['retention.action.retain'] = 'Extend retention';

suite('nuxeo-retain-button', () => {
  let attachEl;

  setup(async () => {
    attachEl = await fixture(html` <nuxeo-retain-button .document=${document}></nuxeo-retain-button> `);
  });

  suite('test _isAvailable', () => {
    test('Should return true if canSetRetention permission is available', async () => {
      sinon.stub(attachEl, 'canSetRetention').returns(true);
      expect(attachEl._isAvailable()).equal(true);
    });

    test('Should return false if canSetRetention permission is not available', async () => {
      sinon.stub(attachEl, 'canSetRetention').returns(false);
      expect(attachEl._isAvailable()).equal(false);
    });
  });

  suite('test _computeLabel', () => {
    test('Should return i18n label for extend retention button', async () => {
      expect(attachEl._computeLabel()).equal('Extend retention');
    });
  });

  suite('test _toggleDialog', () => {
    test('Should toggle dialog', async () => {
      sinon.stub(attachEl.$.dialog, 'toggle');
      attachEl._toggleDialog();
      expect(attachEl.$.dialog.toggle.calledOnce).to.equal(true);
    });
  });

  suite('test _retain', () => {
    test('Should toggle dialog', async () => {
      sinon.stub(attachEl.$.retainOp, 'execute').resolves();
      sinon.spy(attachEl, '_toggleDialog');
      sinon.spy(attachEl, 'dispatchEvent');
      attachEl.until = '2025-05-09';
      attachEl._retain();
      expect(attachEl.$.retainOp.params).to.deep.equal({ until: '2025-05-09' });
      setTimeout(() => {
        expect(attachEl.dispatchEvent.calledOnce).to.equal(true);
        expect(attachEl._toggleDialog.calledOnce).to.equal(true);
      }, 0);
    });
  });

  suite('test _computeMinDate', () => {
    setup(async () => {
      sinon.spy(attachEl, 'set');
    });
    test('Should return min date as retain until date of document if document has retain until date & retention date is not indeterminate', async () => {
      attachEl.document.retainUntil = '2025-05-09';
      sinon.stub(attachEl, 'isRetentionDateIndeterminate').returns(false);
      expect(attachEl._computeMinDate()).equal('2025-05-09');
      expect(attachEl.set.calledWith('until', '2025-05-09')).to.equal(true);
    });

    test("Should return min date as tomorrow's date if document has retain until date but retention date is indeterminate", async () => {
      attachEl.document.retainUntil = '2025-05-09';
      const tomorrow = moment().add(1, 'days');
      sinon.stub(attachEl, 'isRetentionDateIndeterminate').returns(true);
      expect(attachEl._computeMinDate()).equal(moment(tomorrow.toJSON()).format('YYYY-MM-DD'));
      expect(attachEl.set.calledWith('until', undefined)).to.equal(true);
    });

    test("Should return min date as tomorrow's date if document does not have retain until date though retention date is not indeterminate", async () => {
      attachEl.document.retainUntil = null;
      const tomorrow = moment().add(1, 'days');
      sinon.stub(attachEl, 'isRetentionDateIndeterminate').returns(false);
      expect(attachEl._computeMinDate()).equal(moment(tomorrow.toJSON()).format('YYYY-MM-DD'));
      expect(attachEl.set.calledWith('until', undefined)).to.equal(true);
    });

    test("Should return min date as tomorrow's date if document does not have retain until date and retention date is indeterminate", async () => {
      attachEl.document.retainUntil = null;
      const tomorrow = moment().add(1, 'days');
      sinon.stub(attachEl, 'isRetentionDateIndeterminate').returns(true);
      expect(attachEl._computeMinDate()).equal(moment(tomorrow.toJSON()).format('YYYY-MM-DD'));
      expect(attachEl.set.calledWith('until', undefined)).to.equal(true);
    });
  });

  suite('test _isValid', () => {
    test('Should return true if document is available & retainUntil property is valid in document', async () => {
      attachEl.document = document;
      attachEl.document.retainUntil = '2025-05-09';
      attachEl.until = '2025-05-09';
      expect(attachEl._isValid()).equal(true);
    });

    test('Should return false if document is available & retainUntil property is valid in document but until is undefined', async () => {
      attachEl.document = document;
      attachEl.document.retainUntil = '2025-05-09';
      expect(attachEl._isValid()).equal(false);
    });

    test('Should return true if document is available but retainUntil property is not available in document', async () => {
      attachEl.document = document;
      attachEl.until = '2025-05-09';
      expect(attachEl._isValid()).equal(true);
    });
  });
});
