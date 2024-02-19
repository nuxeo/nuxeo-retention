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
import '../elements/nuxeo-attach-rule-button.js';

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
window.nuxeo.I18n.en['retention.rule.attachButton.label.heading'] = 'Attach retention rule';
window.nuxeo.I18n.en['retention.rule.attachButton.attached'] = 'Retention rule attached';

suite('nuxeo-attach-rule-button', () => {
  let attachEl;

  setup(async () => {
    attachEl = await fixture(html` <nuxeo-attach-rule-button .document=${document}></nuxeo-attach-rule-button> `);
  });

  suite('test _isAvailable', () => {
    test('Should return provider object if provider is available', async () => {
      const providerObj = {
        provider: true,
      };
      attachEl.provider = providerObj;
      sinon.stub(attachEl, 'canSetRetention').returns(true);
      expect(attachEl._isAvailable()).equal(providerObj);
    });

    test('Should return true if provider is not available and setRetention permission is present', async () => {
      attachEl.provider = null;
      sinon.stub(attachEl, 'canSetRetention').returns(true);
      expect(attachEl._isAvailable()).equal(true);
    });

    test('Should return false if provider is not available and setRetention permission is not present', async () => {
      attachEl.provider = null;
      sinon.stub(attachEl, 'canSetRetention').returns(false);
      expect(attachEl._isAvailable()).equal(false);
    });
  });

  suite('test _computeLabel', () => {
    test('Should compute and return label', async () => {
      expect(attachEl._computeLabel()).equal('Attach retention rule');
    });
  });

  suite('test _toggleDialog', () => {
    test('Should toggle dialog', async () => {
      sinon.spy(attachEl, 'set');
      const dialogSpy = sinon.spy(attachEl.$.dialog, 'toggle');
      attachEl._toggleDialog();
      expect(attachEl.set).to.have.been.calledOnceWith('rule', undefined);
      expect(dialogSpy).to.have.been.calledOnce;
    });
  });

  suite('test _isValid', () => {
    test('Should return the rule object if rule is present and document is valid', async () => {
      const ruleObj = {
        rule: 'some rule',
      };
      attachEl.document = document;
      attachEl.rule = ruleObj;
      expect(attachEl._isValid()).to.equal(ruleObj);
    });

    test('Should return the rule object if rule is present and provider object is present', async () => {
      const providerObj = {
        provider: true,
      };
      attachEl.provider = providerObj;
      const ruleObj = {
        rule: 'some rule',
      };
      attachEl.document = document;
      attachEl.rule = ruleObj;
      expect(attachEl._isValid()).to.equal(ruleObj);
    });

    test('Should return null if rule is valid but neither provider object nor document is present', async () => {
      const ruleObj = {
        rule: 'some rule',
      };
      attachEl.rule = ruleObj;
      attachEl.provider = null;
      attachEl.document = null;
      expect(attachEl._isValid()).to.equal(null);
    });

    test('Should return document object if rule and provider are not present', async () => {
      attachEl.document = document;
      attachEl.rule = null;
      attachEl.provider = null;
      expect(attachEl._isValid()).to.equal(null);
    });

    test('Should return provider object if rule and document are not present', async () => {
      const providerObj = {
        provider: true,
      };
      attachEl.provider = providerObj;
      attachEl.document = null;
      attachEl.rule = null;
      expect(attachEl._isValid()).to.equal(null);
    });
  });

  suite('test _filterRules', () => {
    test('Should return true if provider is present', async () => {
      const providerObj = {
        provider: true,
      };
      const rule = {};
      attachEl.provider = providerObj;
      expect(attachEl._filterRules(rule)).equals(true);
    });

    test('Should return true if retention rule doc types match the current document doc type', async () => {
      const rule = {
        properties: {
          'retention_rule:docTypes': ['file'],
        },
      };
      attachEl.document.type = 'file';
      expect(attachEl._filterRules(rule)).equals(true);
    });

    test('Should return true if there are no retention rule doc types to match', async () => {
      const rule = {
        properties: {
          'retention_rule:docTypes': ['file'],
        },
      };
      attachEl.document.type = 'file';
      expect(attachEl._filterRules(rule)).equals(true);
    });
  });

  suite('test _ruleResultFormatter', () => {
    test('Should format the result if doc description property exists', async () => {
      const doc = {
        title: 'title1',
        properties: {
          'dc:description': 'description',
        },
      };
      sinon.stub(attachEl, '_escapeHTML').returns('title1');
      expect(attachEl._ruleResultFormatter(doc)).equal(
        'title1<span style="display:block;color:#9a9a9a;word-break:break-all;">title1</span>',
      );
    });

    test('Should return the result without formatting if doc description property does not exists', async () => {
      const doc = {
        title: 'title1',
      };
      sinon.stub(attachEl, '_escapeHTML').returns('title1');
      expect(attachEl._ruleResultFormatter(doc)).equal('title1');
    });
  });

  suite('test _ruleSelectionFormatter', () => {
    test('Should format the result if doc description property exists', async () => {
      const doc = {
        title: 'title1',
      };
      sinon.stub(attachEl, '_escapeHTML').returns('title1');
      expect(attachEl._ruleResultFormatter(doc)).equal('title1');
    });
  });

  suite('test _escapeHTML', () => {
    test('Should return markup as it is if it is not a string', async () => {
      const markup = 3;
      expect(attachEl._escapeHTML(markup)).equal(3);
    });

    test('Should escape certain HTML entities and return formatted markup if it is a string', async () => {
      const markup = 'abc>xyz<efg&';
      expect(attachEl._escapeHTML(markup)).equal('abc&gt;xyz&lt;efg&amp;');
    });
  });

  suite('test _onPollStart', () => {
    test('Should dispatch notify event', async () => {
      sinon.spy(attachEl, 'dispatchEvent');
      attachEl._onPollStart();
      expect(attachEl.dispatchEvent).to.have.been.calledOnceWith(
        new CustomEvent('notify', {
          composed: true,
          bubbles: true,
          detail: { message: 'Attaching retention rule' },
        }),
      );
    });
  });

  suite('test _onResponse', () => {
    test('Should dispatch notify and refresh events once has executed', async () => {
      sinon.stub(attachEl.$.waitEs, 'execute').resolves();
      sinon.spy(attachEl, 'dispatchEvent');
      attachEl._onResponse();
      setTimeout(() => {
        expect(attachEl.dispatchEvent).to.have.been.calledTwice;
      }, 0);
    });
  });

  suite('test _attach', () => {
    test('Should execute Bulk.RunAction api call if provider is present', async () => {
      const providerObj = {
        provider: true,
      };
      const rule = {
        uid: '1',
      };
      attachEl.rule = rule;
      attachEl.provider = providerObj;
      sinon.spy(attachEl, '_toggleDialog');
      sinon.stub(attachEl.$.attachRuleOp, 'execute').resolves();
      sinon.spy(attachEl, 'dispatchEvent');
      attachEl._attach();
      expect(attachEl.$.attachRuleOp.op).equal('Bulk.RunAction');
      expect(attachEl.$.attachRuleOp.input).equal(providerObj);
      expect(attachEl.$.attachRuleOp.async).equal(true);
      expect(attachEl.$.attachRuleOp.params).to.deep.equal({
        action: 'attachRetentionRule',
        parameters: '{"ruleId":"1"}',
      });
      setTimeout(() => {
        expect(attachEl._toggleDialog).to.have.been.calledOnce;
      }, 0);
    });
    test('Should execute Retention.AttachRule api call if provider is not present', async () => {
      const rule = {
        uid: '1',
      };
      attachEl.rule = rule;
      attachEl.provider = null;
      sinon.spy(attachEl, '_toggleDialog');
      sinon.stub(attachEl.$.attachRuleOp, 'execute').resolves();
      sinon.spy(attachEl, 'dispatchEvent');
      attachEl._attach();
      expect(attachEl.$.attachRuleOp.op).equal('Retention.AttachRule');
      expect(attachEl.$.attachRuleOp.input).equal(attachEl.document);
      expect(attachEl.$.attachRuleOp.async).equal(false);
      expect(attachEl.$.attachRuleOp.params).to.deep.equal({ rule: '1' });
      setTimeout(() => {
        expect(attachEl._toggleDialog).to.have.been.calledOnce;
        expect(attachEl.dispatchEvent).to.have.been.calledOnce;
      }, 0);
    });
  });
});
