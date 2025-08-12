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
import '../elements/nuxeo-retain-button.js';
import sinon from 'sinon';
import { expect } from 'chai';

window.nuxeo.I18n.language = 'en';
window.nuxeo.I18n.en = window.nuxeo.I18n.en || {};
window.nuxeo.I18n.en['retention.action.retain'] = 'Extend retention';

suite('nuxeo-retain-button', () => {
  let attachEl;

  setup(async () => {
    attachEl = await fixture(html` <nuxeo-retain-button></nuxeo-retain-button> `);
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
});
