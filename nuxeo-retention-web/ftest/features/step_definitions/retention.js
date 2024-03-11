// eslint-disable-next-line import/no-extraneous-dependencies
import { When, Then } from '@cucumber/cucumber';

When('I set a legal hold on the document with description {string}', async function (desc) {
  const browser = await this.ui.browser;
  await browser.clickDocumentActionMenu('nuxeo-hold-toggle-button:not([hold])');
  const dialog = await browser.el.element('nuxeo-hold-toggle-button #dialog');
  await dialog.waitForVisible();
  const descInput = await dialog.element('nuxeo-textarea[name="description"]');
  await fixtures.layouts.setValue(descInput, desc);
  const ele = await dialog.element('paper-button[name="hold"]');
  await ele.waitForEnabled();
  await ele.click();
});

When('I unset the legal hold on the document', async function () {
  const browser = await this.ui.browser;
  await browser.clickDocumentActionMenu('nuxeo-hold-toggle-button[hold]');
});

Then('I see the document is under legal hold', async function () {
  const browser = await this.ui.browser;
  const documentPage = await browser.documentPage(this.doc.type);
  const infoBar = await documentPage.infoBar;
  await infoBar.waitForVisible();
  const ele = await infoBar.element('#retentionInfoBar #legalHold');
  await ele.waitForVisible();
});

Then('I see the document is not under legal hold', async function () {
  const browser = await this.ui.browser;
  const page = await browser.documentPage(this.doc.type);
  const infoBar = await page.infoBar;
  await infoBar.waitForExist('#legalHold', false);
});

Then('I can unset the legal hold on the document', async function () {
  const browser = await this.ui.browser;
  const ele = await browser.el;
  await ele.waitForExist('nuxeo-hold-toggle-button[hold]');
});

Then('I cannot set the legal hold on the document', async function () {
  const browser = await this.ui.browser;
  const ele = await browser.el;
  await ele.waitForExist('nuxeo-hold-toggle-button', false);
});

Then('I cannot unset the legal hold on the document', async function () {
  const browser = await this.ui.browser;
  const ele = await browser.el;
  await ele.waitForExist('nuxeo-hold-toggle-button[hold]', false);
});

Then('I cannot edit main blob', async function () {
  const browser = await this.ui.browser;
  const page = await browser.documentPage(this.doc.type);
  await page.el.waitForExist('nuxeo-replace-blob-button', false);
  await page.el.waitForExist('nuxeo-delete-blob-button', false);
});

Then('I can edit main blob', async function () {
  const browser = await this.ui.browser;
  const page = await browser.documentPage(this.doc.type);
  await page.el.waitForExist('nuxeo-replace-blob-button');
  await page.el.waitForExist('nuxeo-delete-blob-button');
});

Then('I can see the retention menu', async function () {
  const drawer = await this.ui.drawer;
  const ele = await drawer.el.element('nuxeo-menu-icon[name="retention"]');
  await ele.waitForVisible;
});

Then('I cannot see the retention menu', async function () {
  const drawer = await this.ui.drawer;
  await drawer.waitForNotVisible('nuxeo-menu-icon[name="retention"]');
});

When('I go to the retention event', async function () {
  const drawer = await this.ui.drawer;
  const menu = await drawer.open('retention');
  try {
    // XXX click sometimes hits nuxeo-browser, resulting in an error
    const menuElement = await menu.element('nuxeo-menu-item[name="events"]');
    await menuElement.waitForVisible();
    await menuElement.click();
    return true;
  } catch (e) {
    return false;
  }
});

When('I go to the retention rules location', async function () {
  const drawer = await this.ui.drawer;
  const menu = await drawer.open('retention');
  try {
    // XXX click sometimes hits nuxeo-browser, resulting in an error
    const menuElement = await menu.element('nuxeo-menu-item[name="rules"]');
    await menuElement.waitForVisible();
    await menuElement.click();
    return true;
  } catch (e) {
    return false;
  }
});

When('I navigate to Retention Search page', async function () {
  const drawer = await this.ui.drawer;
  const menu = await drawer.open('retention');
  try {
    // XXX click sometimes hits nuxeo-browser, resulting in an error
    const menuElement = await menu.element('nuxeo-menu-item[name="search"]');
    await menuElement.click();
    return true;
  } catch (e) {
    return false;
  }
});

Then('I attach the {string} rule to the document', async function (ruleName) {
  const browser = await this.ui.browser;
  await browser.clickDocumentActionMenu('nuxeo-attach-rule-button');
  const dialog = await browser.el.element('nuxeo-attach-rule-button #dialog');
  await dialog.waitForVisible();
  const select = await dialog.element('nuxeo-document-suggestion');
  await fixtures.layouts.setValue(select, ruleName);
  const addButton = await dialog.element('paper-button[name="add"]');
  await addButton.waitForEnabled();
  await addButton.click();
  await driver.waitForVisible('iron-overlay-backdrop', 5000, true);
});

Then('I see the document is under retention', async function () {
  const browser = await this.ui.browser;
  const documentPage = await browser.documentPage(this.doc.type);
  const infoBar = await documentPage.infoBar;
  await infoBar.waitForVisible();
  const ele = await infoBar.element('#retentionInfoBar #retention');
  await ele.waitForVisible();
});

Then('I see the document is under retention for {int} days', async function (days) {
  const browser = await this.ui.browser;
  const documentPage = await browser.documentPage(this.doc.type);
  const infoBar = await documentPage.infoBar;
  await infoBar.waitForVisible();
  const ele = await infoBar.element('#retentionInfoBar #retention');
  await ele.waitForVisible();
  const infoBarText = await ele.getText();
  const expectedDate = await moment().add(days, 'days').format(global.dateFormat);
  return infoBarText.indexOf(expectedDate) > 0;
});

Then('I see the document is under indeterminate retention', async function () {
  const browser = await this.ui.browser;
  const documentPage = await browser.documentPage(this.doc.type);
  const infoBar = await documentPage.infoBar;
  await infoBar.waitForVisible();
  const ele = await infoBar.element('#retentionInfoBar #indeterminateRetention');
  await ele.waitForVisible();
});

Then('I cannot see the trash button', async function () {
  const browser = await this.ui.browser;
  const trashButton = await browser.trashDocumentButton;
  const isButtonVisible = await trashButton.isVisible();
  isButtonVisible.should.be.equals(false);
});

When('I have a "ContractEnd" retention event', async () => {
  fixtures.vocabularies.createEntry('RetentionEvent', 'Retention.ContractEnd', {
    obsolete: 0,
    id: 'Retention.ContractEnd',
    label: 'Contract End',
  });
});

When('I fire the {string} retention event with {string} input', async function (eventName, eventInput) {
  const ui = await this.ui;
  const evtsElement = await ui.el.element('nuxeo-retention-events');
  await evtsElement.waitForVisible();
  const eventSelectElt = await evtsElement.element('nuxeo-directory-suggestion[name="event"]');
  await eventSelectElt.waitForVisible();
  const eventInputElt = await evtsElement.element('nuxeo-input[name="eventInput"]');
  await eventInputElt.waitForVisible();
  await fixtures.layouts.setValue(eventSelectElt, eventName);
  await fixtures.layouts.setValue(eventInputElt, eventInput);
  const fireElement = await evtsElement.$('paper-button[name="fire"]');
  await fireElement.waitForEnabled();
  await fireElement.click();
});

When('I search for documents Under legal hold', async function () {
  const ui = await this.ui;
  const ele = await ui.el.element('nuxeo-search-page#retentionSearch');
  await ele.waitForVisible();
  const searchPage = await ui.el.element('nuxeo-search-page#retentionSearch');
  const filterBtn = await searchPage.element('nuxeo-retention-search-results nuxeo-quick-filters');
  await filterBtn.waitForVisible();
  await filterBtn.click();
});

When('I search for documents with Retention Rule {string}', async function (retentionRule) {
  const ui = await this.ui;
  const searchPage = await ui.el.element('nuxeo-search-page#retentionSearch');
  await searchPage.waitForVisible();
  const rulesSelectElt = await searchPage.element('nuxeo-dropdown-aggregation');
  await rulesSelectElt.waitForVisible();
  await fixtures.layouts.setValue(rulesSelectElt, retentionRule);
});

When('I clear the search filters', async function () {
  const ui = await this.ui;
  const searchPage = await ui.el.element('nuxeo-search-page#retentionSearch');
  const clearBtn = await searchPage.element('div.buttons paper-button');
  await clearBtn.click();
});

Then('I can see {int} document in search results', async function (results) {
  await driver.pause(2000);
  const ui = await this.ui;
  const searchPage = await ui.el.element('nuxeo-search-page#retentionSearch');
  await searchPage.waitForVisible();
  const ele = await searchPage.element('nuxeo-retention-search-results span.resultsCount');
  if (results === 0) {
    return !ele.isVisible();
  }
  const text = await ele.getText();
  if (text !== `${results} result(s)`) {
    throw new Error(`Expected count of ${results} but found ${text}`);
  }
  return true;
});

Then('I can see the extend retention action', async function () {
  const browser = await this.ui.browser;
  // await browser.clickDocumentActionMenu('nuxeo-retain-button');
  const menu = await browser.el.element('nuxeo-actions-menu');
  // eslint-disable-next-line no-console
  console.log(menu);
  await menu.waitForExist('nuxeo-retain-button');
  const action = await menu.element('nuxeo-retain-button');
  // eslint-disable-next-line no-console
  console.log(action);
  const action2 = await menu.element('nuxeo-hold-toggle-button');
  // eslint-disable-next-line no-console
  console.log(action2);
  await action.waitForExist();
  if ((await action.getAttribute('show-label')) !== null) {
    const myButton = await menu.element('#dropdownButton');
    await myButton.click();
    await menu.waitForVisible('paper-listbox');
    await menu.waitForVisible('[slot="dropdown"] .label');
    await menu.waitForEnabled('[slot="dropdown"] .label');
  }
  const myClass = await action.$('.action');
  await myClass.waitForVisible();
  await myClass.waitForEnabled();
  await myClass.click();
  const dialog = await browser.el.element('nuxeo-retain-button #dialog');
  await dialog.waitForVisible();
  const button = await dialog.element('paper-button[name = "cancel"]');
  await button.click();
});

Then('I set the retention to expire in {int} days', async function (days) {
  const browser = await this.ui.browser;
  await browser.clickDocumentActionMenu('nuxeo-retain-button');
  const dialog = await browser.el.element('nuxeo-retain-button #dialog');
  await dialog.waitForVisible();
  const dateInput = await dialog.element('#picker');
  const futureDate = await moment().add(days, 'days').format(global.dateFormat);
  await fixtures.layouts.setValue(dateInput, futureDate);
  const addButton = await dialog.element('paper-button[name="add"]');
  await addButton.waitForEnabled();
  await addButton.click();
});

When('I wait {int} seconds', async (seconds) => {
  await driver.pause(seconds * 1000);
});
