# Add Assumption Set for Step 1
curl --location 'http://localhost:8081/api/assumptions' \
--form 'name="3 Step Model Chaining Assumption Set 1"' \
--form 'description="3 Step Model Chaining Assumption Set 1"' \
--form 'basePD="0.03"' \
--form 'baseLGD="0.45"'

# Add Assumption Set for Step 2
curl --location 'http://localhost:8081/api/assumptions' \
--form 'name="3 Step Model Chaining Assumption Set 2"' \
--form 'description="3 Step Model Chaining Assumption Set 2"' \
--form 'baseSpread="0.0015"' \
--form 'riskMultiplier="0.02"'

# Add Assumption Set for Step 3
curl --location 'http://localhost:8081/api/assumptions' \
--form 'name="3 Step Model Chaining Assumption Set 3"' \
--form 'description="3 Step Model Chaining Assumption Set 3"' \
--form 'servicingFee="0.005"' \
--form 'targetMargin="0.12"'

# Add Position File
curl --location 'http://localhost:8080/api/positions/upload' \
--form 'name="3 Steps Chain PF"' \
--form 'positionDate="2025-11-30"' \
--form 'file=@"/Users/NomanAhmed/Documents/Noman/code/github/risk-valuation-platform/test-data/positionfiles/v5/PositionFile_11302025-1loan-tested-chaining/PositionFile_11302025.zip"'

# Add Model Step 1
curl --location 'http://localhost:8082/api/models' \
--header 'Content-Type: application/json' \
--data '{
  "name": "Risk Assessment Model",
  "description": "Step 1: Calculate PD, LGD and base risk score",
  "modelDefinition": {
    "inputs": {
      "positionFields": ["loanId", "creditScore", "ltvRatio", "principal"],
      "assumptionValues": ["basePD", "baseLGD"],
      "csvTables": []
    },
    "derivedFields": [
      {
        "name": "adjustedPD",
        "expression": "assumption.keyLookup.basePD * (creditScore < 650 ? 1.5 : creditScore < 750 ? 1.0 : 0.8)"
      },
      {
        "name": "adjustedLGD",
        "expression": "assumption.keyLookup.baseLGD * (ltvRatio > 0.8 ? 1.2 : 1.0)"
      },
      {
        "name": "riskScore",
        "expression": "adjustedPD * adjustedLGD * 100"
      }
    ],
    "outputs": [
      { "name": "loanId", "expression": "loanId" },
      { "name": "adjustedPD", "expression": "adjustedPD" },
      { "name": "adjustedLGD", "expression": "adjustedLGD" },
      { "name": "riskScore", "expression": "riskScore" }
    ]
  }
}'

# Add Model Step 2
curl --location 'http://localhost:8082/api/models' \
--header 'Content-Type: application/json' \
--data '{
  "name": "Pricing Calculation Model",
  "description": "Step 2: Calculate risk-adjusted rates using risk assessment results",
  "modelDefinition": {
    "inputs": {
      "positionFields": ["loanId", "interestRate", "principal"],
      "assumptionValues": ["baseSpread", "riskMultiplier"],
      "csvTables": []
    },
    "derivedFields": [
      {
        "name": "riskPremium",
        "expression": "risk_riskScore * assumption.keyLookup.riskMultiplier / 100"
      },
      {
        "name": "adjustedRate",
        "expression": "fn.toNumber(interestRate) + fn.toNumber(assumption.keyLookup.baseSpread) + riskPremium"
      },
      {
        "name": "monthlyRate",
        "expression": "adjustedRate / 12"
      }
    ],
    "outputs": [
      { "name": "loanId", "expression": "loanId" },
      { "name": "riskPremium", "expression": "riskPremium" },
      { "name": "adjustedRate", "expression": "adjustedRate" },
      { "name": "monthlyRate", "expression": "monthlyRate" },
      { "name": "risk_riskScore", "expression": "risk_riskScore" }
    ]
  }
}'

# Add Model Step 3
curl --location 'http://localhost:8082/api/models' \
--header 'Content-Type: application/json' \
--data '{
  "name": "Payment Calculation Model Step 3",
  "description": "Step 3: Calculate final payments using pricing results",
  "modelDefinition": {
    "inputs": {
      "positionFields": ["loanId", "principal", "termMonths"],
      "assumptionValues": ["servicingFee"],
      "csvTables": []
    },
    "derivedFields": [
      {
        "name": "monthlyPayment",
        "expression": "pricing_monthlyRate > 0 ? (principal * pricing_monthlyRate / (1 - Math.pow(1 + pricing_monthlyRate, -termMonths))) : 0"
      },
      {
        "name": "totalInterest",
        "expression": "(monthlyPayment * termMonths) - principal"
      },
      {
        "name": "servicingCost",
        "expression": "principal * assumption.keyLookup.servicingFee"
      },
      {
        "name": "netProfit",
        "expression": "totalInterest - servicingCost"
      },
      {
        "name": "profitMargin",
        "expression": "netProfit / principal * 100"
      }
    ],
    "outputs": [
      { "name": "loanId", "expression": "loanId" },
      { "name": "monthlyPayment", "expression": "monthlyPayment" },
      { "name": "totalInterest", "expression": "totalInterest" },
      { "name": "netProfit", "expression": "netProfit" },
      { "name": "profitMargin", "expression": "profitMargin" }
    ]
  }
}'

# Execute Model
curl --location 'http://localhost:8083/api/chains/execute-template' \
--header 'Content-Type: application/json' \
--data '{
  "name": "Complete Loan Processing 3-Step Chain Attempt 7",
  "description": "3-step chain: Risk Assessment to Pricing to Payment Calculation",
  "positionFileId": "800cee0a-3d5a-4459-85b5-93836e060e4b",
  "steps": [
    {
      "modelId": "24542560-0fce-4184-ae0a-7115c35646b1",
      "modelVersion": "1",
      "stepName": "Risk Assessment",
      "description": "Calculate PD, LGD and risk scores",
      "assumptionSetId": "e38f5e0b-82e3-41e9-9904-cccd76e5a3a9",
      "outputFieldsToInclude": ["adjustedPD", "adjustedLGD", "riskScore"],
      "outputPrefix": "risk_"
    },
    {
      "modelId": "9129a0de-7a87-43a3-b8fb-154dcd067f36",
      "modelVersion": "1",
      "stepName": "Pricing Calculation",
      "description": "Calculate risk-adjusted rates and premiums",
      "assumptionSetId": "d963de3d-ea2d-4a47-8836-020d2cb2ec7a",
      "outputFieldsToInclude": ["riskPremium", "adjustedRate", "monthlyRate"],
      "outputPrefix": "pricing_"
    },
    {
      "modelId": "140f3e86-f959-4c3d-a4c9-8404d2e3b1f4",
      "modelVersion": "1",
      "stepName": "Payment Calculation",
      "description": "Calculate final payments and profitability",
      "assumptionSetId": "804caa61-c96e-4eb6-a91d-47a8c7d8372e",
      "outputFieldsToInclude": ["monthlyPayment", "totalInterest", "netProfit", "profitMargin"],
      "outputPrefix": "final_"
    }
  ]
}'

# Execution Results:
# Step 1:
{
    "content": [
        {
            "id": "606d3cf2-abeb-4e91-92c5-b116e14f8d4a",
            "loanId": "LN001",
            "output": {
                "results": {
                    "loanId": "LN001",
                    "riskScore": 1.35,
                    "adjustedPD": 0.03,
                    "adjustedLGD": 0.45
                },
                "positionData": {
                    "principal": 100000.0,
                    "termMonths": 360,
                    "customFields": {
                        "pd": "0.02",
                        "ead": "100000",
                        "lgd": "0.45",
                        "fico": "700",
                        "stage": "1",
                        "region": "Northeast",
                        "ltvRatio": "0.025",
                        "creditScore": "650",
                        "propertyType": "SingleFamily",
                        "scoreOverride": "Yes",
                        "timeToDefault": "2.5",
                        "underwriterName": "Alice"
                    },
                    "interestRate": 5.5,
                    "rateSchedule": [
                        {
                            "rate": 5.5,
                            "effectiveDate": "2020-01-01"
                        }
                    ],
                    "originationDate": "2020-01-01",
                    "paymentSchedule": [
                        {
                            "endDate": "2020-02-01",
                            "startDate": "2020-02-01",
                            "paymentType": "Scheduled",
                            "monthlyPayment": 2000.0,
                            "interestPayment": 1000.0,
                            "principalPayment": 1000.0
                        }
                    ],
                    "amortizationType": "Fixed"
                },
                "derivedAttributes": {
                    "riskScore": 1.35,
                    "adjustedPD": 0.03,
                    "adjustedLGD": 0.45
                }
            },
            "createdAt": "2025-09-26T20:01:59.337689"
        }
    ],
    "pageable": {
        "pageNumber": 0,
        "pageSize": 50,
        "sort": {
            "empty": false,
            "sorted": true,
            "unsorted": false
        },
        "offset": 0,
        "paged": true,
        "unpaged": false
    },
    "last": true,
    "totalPages": 1,
    "totalElements": 1,
    "first": true,
    "size": 50,
    "number": 0,
    "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
    },
    "numberOfElements": 1,
    "empty": false
}

# Step 2:
{
    "content": [
        {
            "id": "a72cb810-5943-4a25-a3e1-65ada1fca688",
            "loanId": "LN001",
            "output": {
                "results": {
                    "loanId": "LN001",
                    "monthlyRate": 0.45848083333333334,
                    "riskPremium": 2.7000000000000006E-4,
                    "adjustedRate": 5.50177,
                    "risk_riskScore": 1.35
                },
                "positionData": {
                    "principal": 100000.0,
                    "termMonths": 360,
                    "customFields": {
                        "pd": "0.02",
                        "ead": "100000",
                        "lgd": "0.45",
                        "fico": "700",
                        "stage": "1",
                        "region": "Northeast",
                        "ltvRatio": "0.025",
                        "creditScore": "650",
                        "propertyType": "SingleFamily",
                        "scoreOverride": "Yes",
                        "timeToDefault": "2.5",
                        "underwriterName": "Alice"
                    },
                    "interestRate": 5.5,
                    "rateSchedule": [
                        {
                            "rate": 5.5,
                            "effectiveDate": "2020-01-01"
                        }
                    ],
                    "originationDate": "2020-01-01",
                    "paymentSchedule": [
                        {
                            "endDate": "2020-02-01",
                            "startDate": "2020-02-01",
                            "paymentType": "Scheduled",
                            "monthlyPayment": 2000.0,
                            "interestPayment": 1000.0,
                            "principalPayment": 1000.0
                        }
                    ],
                    "amortizationType": "Fixed"
                },
                "derivedAttributes": {
                    "monthlyRate": 0.45848083333333334,
                    "riskPremium": 2.7000000000000006E-4,
                    "adjustedRate": 5.50177
                }
            },
            "createdAt": "2025-09-26T20:01:59.338813"
        }
    ],
    "pageable": {
        "pageNumber": 0,
        "pageSize": 50,
        "sort": {
            "empty": false,
            "sorted": true,
            "unsorted": false
        },
        "offset": 0,
        "paged": true,
        "unpaged": false
    },
    "last": true,
    "totalPages": 1,
    "totalElements": 1,
    "first": true,
    "size": 50,
    "number": 0,
    "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
    },
    "numberOfElements": 1,
    "empty": false
}

# Step 3:
{
    "content": [
        {
            "id": "cdb89cbe-39d3-41ca-abfb-922eedb2473d",
            "loanId": "LN001",
            "output": {
                "results": {
                    "loanId": "LN001",
                    "netProfit": 16404810,
                    "profitMargin": 16404.81,
                    "totalInterest": 16405310,
                    "monthlyPayment": 45848.083333333336
                },
                "positionData": {
                    "principal": 100000.0,
                    "termMonths": 360,
                    "customFields": {
                        "pd": "0.02",
                        "ead": "100000",
                        "lgd": "0.45",
                        "fico": "700",
                        "stage": "1",
                        "region": "Northeast",
                        "ltvRatio": "0.025",
                        "creditScore": "650",
                        "propertyType": "SingleFamily",
                        "scoreOverride": "Yes",
                        "timeToDefault": "2.5",
                        "underwriterName": "Alice"
                    },
                    "interestRate": 5.5,
                    "rateSchedule": [
                        {
                            "rate": 5.5,
                            "effectiveDate": "2020-01-01"
                        }
                    ],
                    "originationDate": "2020-01-01",
                    "paymentSchedule": [
                        {
                            "endDate": "2020-02-01",
                            "startDate": "2020-02-01",
                            "paymentType": "Scheduled",
                            "monthlyPayment": 2000.0,
                            "interestPayment": 1000.0,
                            "principalPayment": 1000.0
                        }
                    ],
                    "amortizationType": "Fixed"
                },
                "derivedAttributes": {
                    "netProfit": 16404810,
                    "profitMargin": 16404.81,
                    "servicingCost": 500.0,
                    "totalInterest": 16405310,
                    "monthlyPayment": 45848.083333333336
                }
            },
            "createdAt": "2025-09-26T20:01:59.352511"
        }
    ],
    "pageable": {
        "pageNumber": 0,
        "pageSize": 50,
        "sort": {
            "empty": false,
            "sorted": true,
            "unsorted": false
        },
        "offset": 0,
        "paged": true,
        "unpaged": false
    },
    "last": true,
    "totalPages": 1,
    "totalElements": 1,
    "first": true,
    "size": 50,
    "number": 0,
    "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
    },
    "numberOfElements": 1,
    "empty": false
}
