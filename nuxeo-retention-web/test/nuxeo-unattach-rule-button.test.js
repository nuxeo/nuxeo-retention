/**
@license
©2023 Hyland Software, Inc. and its affiliates. All rights reserved. 
All Hyland product names are registered or unregistered trademarks of Hyland Software, Inc. or its affiliates.

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
import { fixture, html } from '@nuxeo/testing-helpers';
import '../elements/nuxeo-unattach-rule-button.js';
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
window.nuxeo.I18n.en['retention.rule.label.undeclare'] = 'Undeclare Record';
window.nuxeo.I18n.en['retention.rule.label.undeclared.notify'] = 'Record undeclared';

suite('nuxeo-unattach-rule-button', () => {
  let attachEl;

  setup(async () => {
    attachEl = await fixture(html` <nuxeo-unattach-rule-button .document=${document}></nuxeo-unattach-rule-button> `);
  });

  suite('test _isAvailable', () => {
    test('Should return true if document is available & isFlexibleRecord property is true & hasFacet = true & has WriteProperties & UnsetRetention permission', async () => {
      const doc = document;
      doc.isFlexibleRecord = true;
      const permissionStub = sinon.stub(attachEl, 'hasPermission');
      sinon.stub(attachEl, 'hasFacet').returns(true);
      permissionStub.withArgs(doc, 'WriteProperties').returns(true);
      permissionStub.withArgs(doc, 'UnsetRetention').returns(true);
      expect(attachEl._isAvailable(doc)).equal(true);
    });

    test('Should return false if document is available & isFlexibleRecord property is true & hasFacet = true & has WriteProperties but no UnsetRetention permission', async () => {
      const doc = document;
      doc.isFlexibleRecord = true;
      const permissionStub = sinon.stub(attachEl, 'hasPermission');
      sinon.stub(attachEl, 'hasFacet').returns(true);
      permissionStub.withArgs(doc, 'WriteProperties').returns(true);
      permissionStub.withArgs(doc, 'UnsetRetention').returns(false);
      expect(attachEl._isAvailable(doc)).equal(false);
    });

    test('Should return false if document is available & isFlexibleRecord property is true & hasFacet = true but no WriteProperties & UnsetRetention permission', async () => {
      const doc = document;
      doc.isFlexibleRecord = true;
      const permissionStub = sinon.stub(attachEl, 'hasPermission');
      sinon.stub(attachEl, 'hasFacet').returns(true);
      permissionStub.withArgs(doc, 'WriteProperties').returns(false);
      permissionStub.withArgs(doc, 'UnsetRetention').returns(false);
      expect(attachEl._isAvailable(doc)).equal(false);
    });

    test('Should return false if document is available & isFlexibleRecord property is true but hasFacet != true & has WriteProperties & UnsetRetention permission', async () => {
      const doc = document;
      doc.isFlexibleRecord = true;
      const permissionStub = sinon.stub(attachEl, 'hasPermission');
      sinon.stub(attachEl, 'hasFacet').returns(false);
      permissionStub.withArgs(doc, 'WriteProperties').returns(true);
      permissionStub.withArgs(doc, 'UnsetRetention').returns(true);
      expect(attachEl._isAvailable(doc)).equal(false);
    });

    test('Should return false if document is available & isFlexibleRecord property is false but hasFacet = true & has WriteProperties & UnsetRetention permission', async () => {
      const doc = document;
      doc.isFlexibleRecord = false;
      const permissionStub = sinon.stub(attachEl, 'hasPermission');
      sinon.stub(attachEl, 'hasFacet').returns(true);
      permissionStub.withArgs(doc, 'WriteProperties').returns(true);
      permissionStub.withArgs(doc, 'UnsetRetention').returns(true);
      expect(attachEl._isAvailable(doc)).equal(false);
    });
  });

  suite('test _computeLabel', () => {
    test('Should return label for undeclare record', async () => {
      expect(attachEl._computeLabel()).equal('Undeclare Record');
    });
  });

  suite('test _unretain', () => {
    test('Should dispatch notify and document-updated event', async () => {
      sinon.stub(attachEl.$.unretainOp, 'execute').resolves();
      sinon.spy(attachEl, 'dispatchEvent');
      attachEl._unretain();
      setTimeout(() => {
        expect(attachEl.dispatchEvent.calledTwice).to.equal(true);
      }, 0);
    });
  });
});
