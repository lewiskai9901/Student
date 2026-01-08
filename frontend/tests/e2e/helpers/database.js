/**
 * Database verification helper for E2E tests
 * Uses MySQL CLI to verify data consistency
 */

import { exec } from 'child_process';
import { promisify } from 'util';

const execAsync = promisify(exec);

export class DatabaseHelper {
  constructor() {
    this.mysqlPath = 'C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe';
    this.host = 'localhost';
    this.user = 'root';
    this.password = '123456';
    this.database = 'student_management';
  }

  /**
   * Execute SQL query
   * @param {string} query - SQL query to execute
   * @returns {Promise<string>} Query result
   */
  async executeQuery(query) {
    const command = `"${this.mysqlPath}" -u ${this.user} -p${this.password} ${this.database} -e "${query}"`;
    try {
      const { stdout } = await execAsync(command);
      return stdout;
    } catch (error) {
      console.error('Database query failed:', error.message);
      throw error;
    }
  }

  /**
   * Get department by ID
   */
  async getDepartment(id) {
    const query = `SELECT * FROM departments WHERE id = ${id} AND deleted = 0;`;
    return await this.executeQuery(query);
  }

  /**
   * Get department by name
   */
  async getDepartmentByName(name) {
    const query = `SELECT * FROM departments WHERE dept_name = '${name}' AND deleted = 0;`;
    return await this.executeQuery(query);
  }

  /**
   * Count departments
   */
  async countDepartments() {
    const query = `SELECT COUNT(*) as count FROM departments WHERE deleted = 0;`;
    const result = await this.executeQuery(query);
    const match = result.match(/(\d+)/);
    return match ? parseInt(match[1]) : 0;
  }

  /**
   * Get role by ID
   */
  async getRole(id) {
    const query = `SELECT * FROM roles WHERE id = ${id} AND deleted = 0;`;
    return await this.executeQuery(query);
  }

  /**
   * Get role by name
   */
  async getRoleByName(name) {
    const query = `SELECT * FROM roles WHERE role_name = '${name}' AND deleted = 0;`;
    return await this.executeQuery(query);
  }

  /**
   * Count roles
   */
  async countRoles() {
    const query = `SELECT COUNT(*) as count FROM roles WHERE deleted = 0;`;
    const result = await this.executeQuery(query);
    const match = result.match(/(\d+)/);
    return match ? parseInt(match[1]) : 0;
  }

  /**
   * Get user by ID
   */
  async getUser(id) {
    const query = `SELECT * FROM users WHERE id = ${id} AND deleted = 0;`;
    return await this.executeQuery(query);
  }

  /**
   * Get user by username
   */
  async getUserByUsername(username) {
    const query = `SELECT * FROM users WHERE username = '${username}' AND deleted = 0;`;
    return await this.executeQuery(query);
  }

  /**
   * Count users
   */
  async countUsers() {
    const query = `SELECT COUNT(*) as count FROM users WHERE deleted = 0;`;
    const result = await this.executeQuery(query);
    const match = result.match(/(\d+)/);
    return match ? parseInt(match[1]) : 0;
  }

  /**
   * Count students
   */
  async countStudents() {
    const query = `SELECT COUNT(*) as count FROM students;`;
    const result = await this.executeQuery(query);
    const match = result.match(/(\d+)/);
    return match ? parseInt(match[1]) : 0;
  }

  /**
   * Count classes
   */
  async countClasses() {
    const query = `SELECT COUNT(*) as count FROM classes WHERE deleted = 0;`;
    const result = await this.executeQuery(query);
    const match = result.match(/(\d+)/);
    return match ? parseInt(match[1]) : 0;
  }

  /**
   * Count buildings
   */
  async countBuildings() {
    const query = `SELECT COUNT(*) as count FROM buildings WHERE deleted = 0;`;
    const result = await this.executeQuery(query);
    const match = result.match(/(\d+)/);
    return match ? parseInt(match[1]) : 0;
  }

  /**
   * Count dormitories
   */
  async countDormitories() {
    const query = `SELECT COUNT(*) as count FROM dormitories WHERE deleted = 0;`;
    const result = await this.executeQuery(query);
    const match = result.match(/(\d+)/);
    return match ? parseInt(match[1]) : 0;
  }

  /**
   * Verify record exists
   */
  async recordExists(table, condition) {
    const query = `SELECT COUNT(*) as count FROM ${table} WHERE ${condition};`;
    const result = await this.executeQuery(query);
    const match = result.match(/(\d+)/);
    return match && parseInt(match[1]) > 0;
  }

  /**
   * Get last inserted ID
   */
  async getLastInsertId(table, orderBy = 'id') {
    const query = `SELECT id FROM ${table} ORDER BY ${orderBy} DESC LIMIT 1;`;
    const result = await this.executeQuery(query);
    const match = result.match(/(\d+)/);
    return match ? parseInt(match[1]) : null;
  }
}
