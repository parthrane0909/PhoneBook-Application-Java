import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./tests",

  timeout: 120000,

  expect: {
    timeout: 10000,
  },

  reporter: [["list"], ["html", { open: "never" }]],

  retries: process.env.CI ? 2 : 0,

  forbidOnly: !!process.env.CI,

  workers: 1,

  use: {
    baseURL: "http://127.0.0.1:5178",
    headless: true,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },

  projects: [
    {
      name: "chromium",
      use: {
        browserName: "chromium",
      },
    },
  ],

  webServer: [
    {
      command: "node start-api.mjs",
      url: "http://127.0.0.1:8000/contacts/?page=1&limit=1",
      reuseExistingServer: !process.env.CI,
      timeout: 600000,
    },
    {
      command: "npm --prefix ../frontend run dev -- --host 127.0.0.1 --port 5178",
      url: "http://127.0.0.1:5178/contacts",
      reuseExistingServer: true,
      timeout: 120000,
    },
  ],
});
