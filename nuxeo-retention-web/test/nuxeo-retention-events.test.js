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
import '../elements/nuxeo-retention-events.js';
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
window.nuxeo.I18n.en['retention.events.empty'] = 'No past events';
window.nuxeo.I18n.en['retention.events.fired.success'] = 'Event successfully fired';

suite('nuxeo-retention-events', () => {
  let attachEl;

  setup(async () => {
    attachEl = await fixture(html` <nuxeo-retention-events .document=${document}></nuxeo-retention-events> `);
  });

  suite('test _observeStartDate', () => {
    test('Should set valid start and end date & referesh history if start date & end date are available', () => {
      sinon.spy(attachEl, '_refreshHistory');
      attachEl.startDate = '2024-02-20';
      attachEl._observeStartDate();
      expect(attachEl.$.provider.params.startDate).equal('2024-02-20');
      expect(attachEl._refreshHistory.calledTwice).to.equal(true);
    });

    test('Should delete provider start date if it is available & refresh history', async () => {
      attachEl.$.provider.params.startDate = '2024-02-19';
      sinon.spy(attachEl, '_refreshHistory');
      attachEl._observeStartDate();
      expect(attachEl.$.provider.params.startDate).equal(undefined);
      expect(attachEl._refreshHistory.calledOnce).to.equal(true);
    });
  });

  suite('test _observeEndDate', () => {
    test('Should set valid start and end date & referesh history if start date & end date are available', () => {
      sinon.spy(attachEl, '_refreshHistory');
      attachEl.endDate = '2024-02-20';
      attachEl._observeEndDate();
      expect(attachEl.$.provider.params.endDate).equal('2024-02-20');
      expect(attachEl._refreshHistory.calledTwice).to.equal(true);
    });

    test('Should delete provider start date if it is available & refresh history', async () => {
      attachEl.$.provider.params.endDate = '2024-02-19';
      sinon.spy(attachEl, '_refreshHistory');
      attachEl._observeEndDate();
      expect(attachEl.$.provider.params.endDate).equal(undefined);
      expect(attachEl._refreshHistory.calledOnce).to.equal(true);
    });
  });

  suite('test _refreshHistory', () => {
    test('Should fetch retention events table if visible is true', async () => {
      attachEl.visible = true;
      sinon.stub(attachEl.$.table, 'reset');
      sinon.stub(attachEl.$.table, 'fetch').resolves();
      attachEl._refreshHistory(100);
      expect(attachEl.$.provider.page).equal(1);
      expect(attachEl.$.table.reset.calledOnce).to.equal(true);
      setTimeout(() => {
        expect(attachEl.$.table.emptyLabel).equal('No past events');
      }, 100);
    });

    test('Should not fetch retention events table if visible is false', async () => {
      attachEl.visible = false;
      sinon.stub(attachEl.$.table, 'reset');
      sinon.stub(attachEl.$.table, 'fetch').resolves();
      attachEl._refreshHistory(100);
      expect(attachEl.$.table.reset.calledOnce).to.equal(false);
      setTimeout(() => {
        expect(attachEl.$.table.emptyLabel).equal('No past events');
      }, 100);
    });
  });

  suite('test _fire', () => {
    test('Should dispatch notify event and referesh history when operation is executed', async () => {
      sinon.stub(attachEl.$.op, 'execute').resolves();
      sinon.spy(attachEl, 'dispatchEvent');
      sinon.spy(attachEl, '_refreshHistory');
      attachEl._event = 'fire';
      attachEl._eventInput = 'fire-input';
      attachEl._fire();
      expect(attachEl.$.op.params).to.deep.equal({ name: 'fire' });
      expect(attachEl.$.op.input).equal('fire-input');
      setTimeout(() => {
        expect(attachEl.dispatchEvent.calledOnce).to.equal(true);
        expect(attachEl._event).equal(null);
        expect(attachEl._eventInput).equal(null);
        expect(attachEl._refreshHistory.calledWith(100)).to.equal(false);
      }, 0);
    });
  });

  suite('test _canFire', () => {
    test('Should return true if firingEvent = false & _event is valid', async () => {
      attachEl.firingEvent = false;
      attachEl._event = 'some event';
      expect(attachEl._canFire()).equal(true);
    });

    test('Should return false if firingEvent = true & _event is valid', async () => {
      attachEl.firingEvent = true;
      attachEl._event = 'some event';
      expect(attachEl._canFire()).equal(false);
    });

    test('Should return false if firingEvent = false & _event is invalid', async () => {
      attachEl.firingEvent = false;
      attachEl._event = '';
      expect(attachEl._canFire()).equal(false);
    });

    test('Should return false if firingEvent = true & _event is invalid', async () => {
      attachEl.firingEvent = true;
      attachEl._event = '';
      expect(attachEl._canFire()).equal(false);
    });
  });

  suite('test _filterEvents', () => {
    test('Should return true if evt is valid & evt.id is present & evt.id has the string "Retention."', async () => {
      const evt = {
        id: 'Retention.AttachRule',
      };
      expect(attachEl._filterEvents(evt)).equal(true);
    });

    test('Should return false if evt is valid & evt.id is present but evt.id does not have the string "Retention."', async () => {
      const evt = {
        id: 'label',
      };
      expect(attachEl._filterEvents(evt)).equal(false);
    });

    test('Should return false if evt is valid but evt.id is not present', async () => {
      const evt = {
        label: 'Retention',
      };
      expect(attachEl._filterEvents(evt)).equal(undefined);
    });

    test('Should return false if evt is invalid', async () => {
      expect(attachEl._filterEvents()).equal(undefined);
    });
  });
});
