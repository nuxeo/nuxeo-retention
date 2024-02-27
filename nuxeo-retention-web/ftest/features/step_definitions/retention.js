// eslint-disable-next-line import/no-extraneous-dependencies
import { When, Then } from '@cucumber/cucumber';

When('I set a legal hold on the document with description {string}', async function (desc) {
  await this.ui.browser.clickDocumentActionMenu('nuxeo-hold-toggle-button:not([hold])');
  const dialog = await this.ui.browser.el.element('nuxeo-hold-toggle-button #dialog');
  await dialog.waitForVisible();
  const descInput = await dialog.element('nuxeo-textarea[name="description"]');
  await fixtures.layouts.setValue(descInput, desc);
  const ele = await dialog.element('paper-button[name="hold"]');
  await ele.waitForEnabled();
  await ele.click();
});

When('I unset the legal hold on the document', async function () {
  await this.ui.browser.clickDocumentActionMenu('nuxeo-hold-toggle-button[hold]');
});

Then('I see the document is under legal hold', async function () {
  const documentPage = await this.ui.browser.documentPage(this.doc.type);
  const infoBar = await documentPage.infoBar;
  await infoBar.waitForVisible();
  const ele = await infoBar.element('#retentionInfoBar #legalHold');
  await ele.waitForVisible();
});

Then('I see the document is not under legal hold', async function () {
  const page = await this.ui.browser.documentPage(this.doc.type);
  const infoBar = await page.infoBar;
  await infoBar.waitForExist('#legalHold', false);
});

Then('I can unset the legal hold on the document', async function () {
  const ele = await this.ui.browser.el;
  await ele.waitForExist('nuxeo-hold-toggle-button[hold]');
});

Then('I cannot set the legal hold on the document', async function () {
  const ele = await this.ui.browser.el;
  await ele.waitForExist('nuxeo-hold-toggle-button', false);
});

Then('I cannot unset the legal hold on the document', async function () {
  const ele = await this.ui.browser.el;
  await ele.waitForExist('nuxeo-hold-toggle-button[hold]', false);
});

Then('I cannot edit main blob', async function () {
  const page = await this.ui.browser.documentPage(this.doc.type);
  await page.el.waitForExist('nuxeo-replace-blob-button', false);
  await page.el.waitForExist('nuxeo-delete-blob-button', false);
});

Then('I can edit main blob', async function () {
  const page = await this.ui.browser.documentPage(this.doc.type);
  await page.el.waitForExist('nuxeo-replace-blob-button');
  await page.el.waitForExist('nuxeo-delete-blob-button');
});

Then('I can see the retention menu', async function () {
  const drawer = await this.ui.drawer;
  const ele = await drawer.el.$('nuxeo-menu-icon[name="retention"]');
  await ele.waitForVisible;
});

Then('I cannot see the retention menu', async function () {
  const drawer = await this.ui.drawer;
  await drawer.waitForNotVisible('nuxeo-menu-icon[name="retention"]');
});

When('I go to the retention event', async function () {
  const menu = await this.ui.drawer.open('retention');
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
  const menu = await this.ui.drawer.open('retention');
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
  const menu = await this.ui.drawer.open('retention');
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
  await this.ui.browser.clickDocumentActionMenu('nuxeo-attach-rule-button');
  const dialog = await this.ui.browser.el.element('nuxeo-attach-rule-button #dialog');
  await dialog.waitForVisible();
  const select = await dialog.element('nuxeo-document-suggestion');
  await fixtures.layouts.setValue(select, ruleName);
  const addButton = await dialog.element('paper-button[name="add"]');
  await addButton.waitForEnabled();
  await addButton.click();
  await driver.waitForVisible('iron-overlay-backdrop', 5000, true);
});

Then('I see the document is under retention', async function () {
  const documentPage = await this.ui.browser.documentPage(this.doc.type);
  const infoBar = await documentPage.infoBar;
  await infoBar.waitForVisible();
  const ele = await infoBar.element('#retentionInfoBar #retention');
  await ele.waitForVisible();
});

Then('I see the document is under retention for {int} days', async function (days) {
  const documentPage = await this.ui.browser.documentPage(this.doc.type);
  const infoBar = await documentPage.infoBar;
  await infoBar.waitForVisible();
  const ele = await infoBar.element('#retentionInfoBar #retention');
  await ele.waitForVisible();
  const infoBarText = await ele.getText();
  const expectedDate = await moment().add(days, 'days').format(global.dateFormat);
  return infoBarText.indexOf(expectedDate) > 0;
});

Then('I see the document is under indeterminate retention', async function () {
  const documentPage = await this.ui.browser.documentPage(this.doc.type);
  const infoBar = await documentPage.infoBar;
  await infoBar.waitForVisible();
  const ele = await infoBar.element('#retentionInfoBar #indeterminateRetention');
  await ele.waitForVisible();
});

Then('I cannot see the trash button', async function () {
  const trashButton = await this.ui.browser.trashDocumentButton;
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
  const evtsElement = await this.ui.el.element('nuxeo-retention-events');
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
  const ele = await this.ui.el.element('nuxeo-search-page#retentionSearch');
  await ele.waitForVisible();
  const searchPage = await this.ui.el.element('nuxeo-search-page#retentionSearch');
  const filterBtn = await searchPage.element('nuxeo-retention-search-results nuxeo-quick-filters');
  await filterBtn.waitForVisible();
  await filterBtn.click();
});

When('I search for documents with Retention Rule {string}', async function (retentionRule) {
  const searchPage = await this.ui.el.element('nuxeo-search-page#retentionSearch');
  await searchPage.waitForVisible();
  const rulesSelectElt = await searchPage.element('nuxeo-dropdown-aggregation');
  await rulesSelectElt.waitForVisible();
  await fixtures.layouts.setValue(rulesSelectElt, retentionRule);
});

When('I clear the search filters', async function () {
  const ele = await this.ui.el.$('nuxeo-search-page#retentionSearch');
  await ele.waitForVisible();
  const searchPage = await this.ui.el.element('nuxeo-search-page#retentionSearch');
  const clearBtn = await searchPage.element('div.buttons paper-button');
  await clearBtn.click();
});

Then('I can see {int} document in search results', async function (results) {
  const searchPage = await this.ui.el.element('nuxeo-search-page#retentionSearch');
  await searchPage.waitForVisible();
  if (results === 0) {
    const ele = await searchPage.element('nuxeo-retention-search-results span.resultsCount');
    return !ele.isVisible();
  }
  return driver.waitUntil(
    async () =>
      (await searchPage.element('nuxeo-retention-search-results span.resultsCount').getText()) ===
      `${results} result(s)`,
  );
});

Then('I can see the extend retention action', async function () {
  await this.ui.browser.clickDocumentActionMenu('nuxeo-retain-button');
  const dialog = await this.ui.browser.el.element('nuxeo-retain-button #dialog');
  await dialog.waitForVisible();
  const button = await dialog.element('paper-button[name = "cancel"]');
  await button.click();
});

Then('I set the retention to expire in {int} days', async function (days) {
  await this.ui.browser.clickDocumentActionMenu('nuxeo-retain-button');
  const dialog = await this.ui.browser.el.element('nuxeo-retain-button #dialog');
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
