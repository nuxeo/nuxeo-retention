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
import '../elements/nuxeo-hold-toggle-button.js';
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
window.nuxeo.I18n.en['retention.holdToggleButton.tooltip.hold'] = 'Legal Hold';
window.nuxeo.I18n.en['retention.holdToggleButton.tooltip.unhold'] = 'Unhold';
window.nuxeo.I18n.en['retention.holdToggleButton.bulk.hold.poll'] = 'Setting legal hold';
window.nuxeo.I18n.en['retention.holdToggleButton.bulk.hold'] = 'Legal hold set';
window.nuxeo.I18n.en['retention.holdToggleButton.bulk.unhold'] = 'Legal hold unset';
window.nuxeo.I18n.en['retention.holdToggleButton.bulk.unhold.poll'] = 'Unsetting legal hold';

suite('nuxeo-hold-toggle-button', () => {
  let attachEl;

  setup(async () => {
    attachEl = await fixture(html` <nuxeo-hold-toggle-button .document=${document}></nuxeo-hold-toggle-button> `);
  });

  suite('test _isAvailable', () => {
    test('Should return provider object if provider is available', async () => {
      const providerObj = {
        provider: true,
      };
      attachEl.provider = providerObj;
      sinon.stub(attachEl, 'canSetLegalHold').returns(true);
      expect(attachEl._isAvailable()).equal(providerObj);
    });

    test('Should return true if provider is not available and canSetLegalHold permission is present', async () => {
      attachEl.provider = null;
      sinon.stub(attachEl, 'canSetLegalHold').returns(true);
      expect(attachEl._isAvailable()).equal(true);
    });

    test('Should return false if provider is not available and canSetLegalHold permission is not present', async () => {
      attachEl.provider = null;
      sinon.stub(attachEl, 'canSetLegalHold').returns(false);
      expect(attachEl._isAvailable()).equal(false);
    });
  });

  suite('test _hold', () => {
    const windowStub = sinon.stub(window, 'confirm');
    test('Should execute Bulk.RunAction if user confirms to put document under legal hold and provider is present', async () => {
      const providerObj = {
        provider: true,
      };
      attachEl.provider = providerObj;
      attachEl.document.isFlexibleRecord = true;
      attachEl.document.isUnderRetentionOrLegalHold = true;
      attachEl.description = 'some text';
      windowStub.returns(true);
      sinon.spy(attachEl, '_toggleDialog');
      sinon.stub(attachEl.$.opHold, 'execute').resolves();
      sinon.spy(attachEl, 'dispatchEvent');
      attachEl._hold();
      expect(attachEl.$.opHold.op).equal('Bulk.RunAction');
      expect(attachEl.$.opHold.input).equal(providerObj);
      expect(attachEl.$.opHold.async).equal(true);
      setTimeout(() => {
        expect(attachEl._toggleDialog.calledOnce).to.equal(true);
      }, 0);
    });

    test('Should execute Document.Hold if user confirms to put document under legal hold and provider is not present', async () => {
      attachEl.provider = null;
      attachEl.document.isFlexibleRecord = true;
      attachEl.document.isUnderRetentionOrLegalHold = true;
      attachEl.description = 'some text';
      sinon.spy(attachEl, '_toggleDialog');
      sinon.stub(attachEl.$.opHold, 'execute').resolves();
      sinon.spy(attachEl, 'dispatchEvent');
      attachEl._hold();
      expect(attachEl.$.opHold.op).equal('Document.Hold');
      expect(attachEl.$.opHold.input).equal(attachEl.document);
      expect(attachEl.$.opHold.async).equal(false);
      expect(attachEl.$.opHold.params).to.deep.equal({ description: 'some text' });
      setTimeout(() => {
        expect(attachEl._toggleDialog.calledOnce).to.equal(true);
        expect(attachEl.dispatchEvent.calledOnce).to.equal(true);
      }, 0);
    });

    test('Should not execute either Bulk.RunAction or Document.Hold if user cancels legal hold on document', async () => {
      sinon.spy(attachEl, '_toggleDialog');
      sinon.stub(attachEl.$.opHold, 'execute');
      attachEl.document.isFlexibleRecord = true;
      attachEl.document.isUnderRetentionOrLegalHold = true;
      windowStub.returns(false);
      attachEl._hold();
      expect(attachEl._toggleDialog.calledOnce).to.equal(false);
      expect(attachEl.$.opHold.execute.calledOnce).to.equal(false);
    });
  });

  suite('test _unhold', () => {
    test('Should execute Bulk.RunAction if provider is present', async () => {
      const providerObj = {
        provider: true,
      };
      attachEl.provider = providerObj;
      sinon.stub(attachEl.$.opUnhold, 'execute');
      attachEl._unhold();
      expect(attachEl.$.opUnhold.op).equal('Bulk.RunAction');
      expect(attachEl.$.opUnhold.input).equal(providerObj);
      expect(attachEl.$.opUnhold.async).equal(true);
      expect(attachEl.$.opUnhold.params).to.deep.equal({ action: 'unholdDocumentsAction' });
      expect(attachEl.$.opUnhold.execute.calledOnce).to.equal(true);
    });

    test('Should execute Document.Unhold if provider is not present', async () => {
      attachEl.provider = null;
      sinon.stub(attachEl.$.opUnhold, 'execute').resolves();
      sinon.spy(attachEl, 'dispatchEvent');
      attachEl._unhold();
      expect(attachEl.$.opUnhold.op).equal('Document.Unhold');
      expect(attachEl.$.opUnhold.input).equal(attachEl.document);
      expect(attachEl.$.opUnhold.async).equal(false);
      expect(attachEl.$.opUnhold.params).to.deep.equal({});
      setTimeout(() => {
        expect(attachEl.dispatchEvent.calledOnce).to.equal(true);
      }, 0);
    });
  });

  suite('test _unhold', () => {
    test('Should toggle dialog if hold is set to false', async () => {
      attachEl.hold = false;
      sinon.spy(attachEl, '_toggleDialog');
      attachEl._toggle();
      expect(attachEl._toggleDialog.calledOnce).to.equal(true);
    });

    test('Should perform unhold operation if hold is set to true', async () => {
      attachEl.hold = true;
      sinon.spy(attachEl, '_unhold');
      attachEl._toggle();
      expect(attachEl._unhold.calledOnce).to.equal(true);
    });
  });

  suite('test _toggleDialog', () => {
    test('Should reset popup and toggle dialog', async () => {
      sinon.spy(attachEl, '_resetPopup');
      sinon.stub(attachEl.$.dialog, 'toggle');
      attachEl._toggleDialog();
      expect(attachEl._resetPopup.calledOnce).to.equal(true);
      expect(attachEl.$.dialog.toggle.calledOnce).to.equal(true);
    });
  });

  suite('test _resetPopup', () => {
    test('Should set description to null', async () => {
      sinon.spy(attachEl, 'set');
      attachEl._resetPopup();
      expect(attachEl.set.calledWith('description', null)).to.equal(true);
    });
  });

  suite('test _computeTooltip', () => {
    test('Should return tooltip text for hold', async () => {
      attachEl.hold = true;
      expect(attachEl._computeTooltip()).equal('Unhold');
    });

    test('Should return tooltip text for unhold', async () => {
      attachEl.hold = false;
      expect(attachEl._computeTooltip()).equal('Legal Hold');
    });
  });

  suite('test _computeLabel', () => {
    test('Should return label for hold', async () => {
      attachEl.hold = true;
      expect(attachEl._computeLabel()).equal('Unhold');
    });

    test('Should return label for unhold', async () => {
      attachEl.hold = false;
      expect(attachEl._computeLabel()).equal('Legal Hold');
    });
  });

  suite('test _computeIcon', () => {
    test('Should return label for hold', async () => {
      attachEl.hold = true;
      expect(attachEl._computeIcon()).equal('nuxeo:hold');
    });

    test('Should return label for unhold', async () => {
      attachEl.hold = false;
      expect(attachEl._computeIcon()).equal('nuxeo:unhold');
    });
  });

  suite('test _documentChanged', () => {
    test('Should set hold property to true if document is present and has legal hold', async () => {
      attachEl.document = document;
      attachEl.document.hasLegalHold = true;
      attachEl._documentChanged();
      expect(attachEl.hold).equal(true);
    });

    test('Should set hold property to true if document is present but does not have legal hold', async () => {
      attachEl.document = document;
      attachEl.document.hasLegalHold = false;
      attachEl._documentChanged();
      expect(attachEl.hold).equal(false);
    });
  });

  suite('test _onHoldPollStart', () => {
    test('Should dispatch notify event', async () => {
      sinon.spy(attachEl, 'dispatchEvent');
      attachEl._onHoldPollStart();
      expect(
        attachEl.dispatchEvent.calledWith(
          new CustomEvent('notify', {
            composed: true,
            bubbles: true,
            detail: { message: 'Setting legal hold' },
          }),
        ),
      ).to.equal(true);
    });
  });

  suite('test _onUnholdPollStart', () => {
    test('Should dispatch notify event', async () => {
      sinon.spy(attachEl, 'dispatchEvent');
      attachEl._onUnholdPollStart();
      expect(
        attachEl.dispatchEvent.calledWith(
          new CustomEvent('notify', {
            composed: true,
            bubbles: true,
            detail: { message: 'Unsetting legal hold' },
          }),
        ),
      ).to.equal(true);
    });
  });

  suite('test _onHoldResponse', () => {
    test('Should dispatch notify and refresh event', async () => {
      sinon.stub(attachEl.$.waitEs, 'execute').resolves();
      sinon.spy(attachEl, 'dispatchEvent');
      attachEl._onHoldResponse();
      setTimeout(() => {
        expect(attachEl.dispatchEvent.calledTwice).to.equal(true);
      }, 0);
    });
  });

  suite('test _onUnholdResponse', () => {
    test('Should dispatch notify and refresh event', async () => {
      sinon.stub(attachEl.$.waitEs, 'execute').resolves();
      sinon.spy(attachEl, 'dispatchEvent');
      attachEl._onUnholdResponse();
      setTimeout(() => {
        expect(attachEl.dispatchEvent.calledTwice).to.equal(true);
      }, 0);
    });
  });
});
