const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read accounts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("accounts").get());
});

test("Authenticated user: Alice cannot read Bob's accounts", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("accounts").get());
});

test("Authenticated user: Alice can create and read her own account", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const accRef = aliceDb.collection("users").doc(ALICE_UID).collection("accounts").doc("acc_1");
  await assertSucceeds(
    accRef.set({
      userId: ALICE_UID,
      name: "Efectivo",
      type: "CASH",
      currentBalance: 500,
      initialBalance: 500,
      currency: "$",
      colorHex: "#4CAF50",
      iconName: "cash",
      createdAt: new Date(),
    })
  );
  await assertSucceeds(accRef.get());
});

test("Authenticated user: Alice can create and read a planned commitment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const comRef = aliceDb.collection("users").doc(ALICE_UID).collection("commitments").doc("com_1");
  await assertSucceeds(
    comRef.set({
      userId: ALICE_UID,
      title: "Laptop en cuotas",
      totalAmount: 1200,
      remainingAmount: 1200,
      paymentMode: "INSTALLMENTS",
      category: "Tecnología",
      totalInstallments: 12,
      paidInstallments: 0,
      installmentAmount: 100,
      frequency: "MONTHLY",
      dueDate: new Date(),
      status: "ACTIVE",
      createdAt: new Date(),
    })
  );
  await assertSucceeds(comRef.get());
});
