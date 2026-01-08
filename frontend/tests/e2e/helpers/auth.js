/**
 * Authentication helper for E2E tests
 */

export class AuthHelper {
  constructor(page) {
    this.page = page;
  }

  /**
   * Login to the system
   * @param {string} username - Username (default: 'admin')
   * @param {string} password - Password (default: 'admin123')
   */
  async login(username = 'admin', password = 'admin123') {
    await this.page.goto('/login');

    // Wait for login form
    await this.page.waitForSelector('input', { timeout: 10000 });

    // Fill in credentials - use more flexible selectors
    const inputs = await this.page.$$('input');
    if (inputs.length >= 2) {
      await inputs[0].fill(username);
      await inputs[1].fill(password);
    } else {
      // Fallback to type selectors
      await this.page.fill('input[type="text"], input:not([type="password"])', username);
      await this.page.fill('input[type="password"]', password);
    }

    // Click login button - try multiple selectors
    const loginButtonSelectors = [
      'button:has-text("登录")',
      'button:has-text("Login")',
      'button[type="submit"]',
      '.login-button',
      '.el-button--primary'
    ];

    let clicked = false;
    for (const selector of loginButtonSelectors) {
      try {
        const button = await this.page.$(selector);
        if (button && await button.isVisible()) {
          await button.click();
          clicked = true;
          break;
        }
      } catch (e) {
        // Continue to next selector
      }
    }

    if (!clicked) {
      throw new Error('Could not find login button');
    }

    // Wait for navigation to complete (either to home or dashboard)
    await this.page.waitForTimeout(3000);

    console.log(`✓ Logged in as ${username}`);
  }

  /**
   * Logout from the system
   */
  async logout() {
    // Look for user menu or logout button
    const logoutSelectors = [
      'text=退出登录',
      'text=退出',
      '[aria-label="退出"]',
      '.logout-btn'
    ];

    for (const selector of logoutSelectors) {
      try {
        const element = await this.page.$(selector);
        if (element) {
          await element.click();
          await this.page.waitForURL('**/login', { timeout: 5000 });
          console.log('✓ Logged out successfully');
          return;
        }
      } catch (e) {
        // Continue to next selector
      }
    }

    console.log('⚠ Logout button not found, clearing storage');
    await this.page.context().clearCookies();
    await this.page.evaluate(() => localStorage.clear());
  }

  /**
   * Get stored token from localStorage
   */
  async getToken() {
    return await this.page.evaluate(() => localStorage.getItem('token'));
  }

  /**
   * Check if user is logged in
   */
  async isLoggedIn() {
    const token = await this.getToken();
    return !!token;
  }
}
