package com.atlassian.performance.tools.jiraactions.page

import com.atlassian.performance.tools.jiraactions.api.page.JiraErrors
import com.atlassian.performance.tools.jiraactions.api.page.form.IssueForm
import com.atlassian.performance.tools.jiraactions.api.page.isElementPresent
import com.atlassian.performance.tools.jiraactions.api.page.tolerateDirtyFormsOnCurrentPage
import com.atlassian.performance.tools.jiraactions.api.page.wait
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.ExpectedConditions.*
import org.openqa.selenium.support.ui.Select
import java.time.Duration
import java.util.function.Supplier

internal class IssueCreateDialog(
    private val driver: WebDriver
) {
    private val form = IssueForm(By.cssSelector("form[name=jiraform]"), driver)
    private val projectField = SingleSelect(driver, By.id("project-field"))
    private val issueTypeField = SingleSelect(driver, By.id("issuetype-field"))
    private val configColumnField = By.id("qf-field-picker-trigger")
    private val resolutionField = By.id("resolution")

    fun waitForDialog(): IssueCreateDialog {
        val jiraErrors = JiraErrors(driver)
        driver.wait(
            timeout = Duration.ofSeconds(30),
            condition = or(
                visibilityOfElementLocated(By.id("create-issue-dialog")),
                jiraErrors.anyCommonError()
            )
        )
        jiraErrors.assertNoErrors()
        driver.tolerateDirtyFormsOnCurrentPage()
        return this
    }

    fun selectProject(projectName: String) = form.waitForRefresh(Supplier {
        projectField.select(projectName)
        return@Supplier this
    })

    fun selectIssueType(issueType: String) = form.waitForRefresh(Supplier {
        issueTypeField.select(issueType)
        return@Supplier this
    })

    fun getIssueTypes() = issueTypeField.getSuggestions()
        .plus(issueTypeField.getCurrentValue())

    fun fill(fieldId: String, value: String): IssueCreateDialog {
        driver.wait(elementToBeClickable(By.id(fieldId))).sendKeys(value)
        return this
    }

    /**
     * Click 'Configure Fields' to display 'All' to ensure
     * all mandatory fields are displayed in creation dialog.
     *
     */
    fun fillRequiredFields(): IssueCreateDialog {
        driver.wait(elementToBeClickable(configColumnField)).click()
        val locator = By.xpath("//div[@id='inline-dialog-field_picker_popup']//a[text()='All']")
        if (driver.isElementPresent(locator)) {
            driver.wait(elementToBeClickable(locator)).click()
        } else {
            driver.wait(elementToBeClickable(configColumnField)).click()
        }
        form.fillRequiredFields()
        selectResolution()
        return this
    }

    fun submit() {
        driver.wait(elementToBeClickable(By.id("create-issue-submit"))).click()
        driver.wait(Duration.ofSeconds(30), invisibilityOfElementLocated(By.className("aui-blanket")))
    }

    /**
     * 'resolution' field is mandatory for one of the xlarge datasets.
     *
     */
    private fun selectResolution() {
        if (driver.isElementPresent(resolutionField)) {
            val dropDown = Select(driver.findElement(resolutionField))
            if (dropDown.options != null && dropDown.options.size > 1) {
                val selection = dropDown.options.get(1)
                dropDown.selectByVisibleText(selection.text)
            }
        }
    }

}
