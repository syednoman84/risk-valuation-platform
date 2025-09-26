# Add Assumption Set
curl --location 'http://localhost:8081/api/assumptions' \
--form 'name="Assumption Set v4"' \
--form 'description="Combined Model Assumption V1 V2 V3"' \
--form 'base_rate="0.99"' \
--form 'prepayment_enabled="false"' \
--form 'stress_factor="1.10"' \
--form 'superKart=@"/Users/NomanAhmed/Documents/Noman/code/github/risk-valuation-platform/test-data/assumptions/v4-combined/superKart.csv"' \
--form 'prepayment_grid=@"/Users/NomanAhmed/Documents/Noman/code/github/risk-valuation-platform/test-data/assumptions/v4-combined/prepayment_grid.csv"' \
--form 'base_annual_rate="0.08"' \
--form 'default_term_months="120"' \
--form 'discountRate="0.05"' \
--form 'macroeconomicAdjustment="1.2"' \
--form 'minimumProvision="0.001"' \
--form 'credit_adjustments=@"/Users/NomanAhmed/Documents/Noman/code/github/risk-valuation-platform/test-data/assumptions/v4-combined/credit_adjustments.csv"' \
--form 'default_matrix=@"/Users/NomanAhmed/Documents/Noman/code/github/risk-valuation-platform/test-data/assumptions/v4-combined/default_matrix.csv"'

# Add Position File
curl --location 'http://localhost:8080/api/positions/upload' \
--form 'name="V4"' \
--form 'positionDate="2025-11-30"' \
--form 'file=@"/Users/NomanAhmed/Documents/Noman/code/github/risk-valuation-platform/test-data/positionfiles/v4-combined/PositionFile_11302025.zip"'

# Add Model
curl --location 'http://localhost:8082/api/models' \
--header 'Content-Type: application/json' \
--data '{
  "name": "V1 V2 and V3 Models Combined V4",
  "description": "Combining 3 models to test all expressions together",
  "modelDefinition": {
    "inputs": {
      "positionFields": ["loanId", "pd", "lgd", "ead", "stage", "timeToDefault", "principal","interestRate","termMonths","originationDate","creditScore","ltvRatio"],
      "assumptionValues": ["discountRate", "macroeconomicAdjustment", "minimumProvision", "base_annual_rate", "default_term_months"],
      "csvTables": ["credit_adjustments","default_matrix"]
    },
    "derivedFields": [
      {
        "name": "discountFactor",
        "expression": "1 / Math.pow(1 + assumption.keyLookup.discountRate, timeToDefault)"
      },
      {
        "name": "baseECL",
        "expression": "pd * lgd * ead * discountFactor"
      },
      {
        "name": "stageMultiplier",
        "expression": "(stage == 1) ? 1.0 : (stage == 2) ? 2.5 : 5.0"
      },
      {
        "name": "adjustedECL",
        "expression": "baseECL * assumption.keyLookup.macroeconomicAdjustment * stageMultiplier"
      },
      {
        "name": "finalECL",
        "expression": "Math.max(adjustedECL, assumption.keyLookup.minimumProvision * ead)"
      },
      {
        "name": "annualRate_v2",
        "expression": "(interestRate != null) ? interestRate : 5.0"
      },
      {
        "name": "term_v2",
        "expression": "(termMonths != null) ? termMonths : 360"
      },
      {
        "name": "creditAdjustment_v2",
        "expression": "fn.toNumber(assumption.tableLookup.lookup('\''credit_adjustments'\'', '\''credit_score'\'', creditScore, '\''rate_adjustment'\''))"
      },
      {
        "name": "ltvBucket_v2",
        "expression": "(ltvRatio <= 0.8) ? '\''low'\'' : (ltvRatio <= 0.9) ? '\''medium'\'' : '\''high'\''"
      },
      {
        "name": "termBucket_v2",
        "expression": "(term_v2 <= 180) ? '\''short'\'' : (term_v2 <= 300) ? '\''medium'\'' : '\''long'\''"
      },
      {
        "name": "defaultRate_v2",
        "expression": "fn.toNumber(assumption.tableLookup.lookup('\''default_matrix'\'', '\''ltv_bucket'\'', ltvBucket_v2, '\''long_term'\''))"
      },
      {
        "name": "adjustedRate_v2",
        "expression": "annualRate_v2 + (creditAdjustment_v2 != null ? creditAdjustment_v2 : 0)"
      },
      {
        "name": "monthlyRate_v2",
        "expression": "adjustedRate_v2 / 12"
      },
      {
        "name": "monthlyInterest_v2",
        "expression": "principal * monthlyRate_v2"
      },
      {
        "name": "pmt_v2",
        "expression": "(term_v2 != null && monthlyRate_v2 != null && monthlyRate_v2 > 0) ? (principal * monthlyRate_v2 / (1 - Math.pow(1 + monthlyRate_v2, -term_v2))) : null"
      },
      {
        "name": "annualRate_v1",
        "expression": "(interestRate != null) ? interestRate : assumption.keyLookup.base_annual_rate"
      },
      {
        "name": "term_v1",
        "expression": "(termMonths != null) ? termMonths : assumption.keyLookup.default_term_months"
      },
      {
        "name": "monthlyRate_v1",
        "expression": "annualRate_v1 / 12"
      },
      {
        "name": "monthlyInterest_v1",
        "expression": "principal * monthlyRate_v1"
      },
      {
        "name": "pmt_v1",
        "expression": "(term_v1 != null && monthlyRate_v1 != null && monthlyRate_v1 > 0) ? (principal * monthlyRate_v1 / (1 - Math.pow(1 + monthlyRate_v1, -term_v1))) : null"
      }
    ],
    "outputs": [
      { "name": "loanId", "expression": "loanId" },
      { "name": "pd_v3", "expression": "pd" },
      { "name": "lgd_v3", "expression": "lgd" },
      { "name": "ead_v3", "expression": "ead" },
      { "name": "stage_v3", "expression": "stage" },
      { "name": "timeToDefault_v3", "expression": "timeToDefault" },
      { "name": "discountFactor_v3", "expression": "discountFactor" },
      { "name": "baseECL_v3", "expression": "baseECL" },
      { "name": "stageMultiplier_v3", "expression": "stageMultiplier" },
      { "name": "adjustedECL_v3", "expression": "adjustedECL" },
      { "name": "finalECL_v3", "expression": "finalECL" },
      { "name": "principal_v2",        "expression": "principal" },
      { "name": "creditScore_v2",      "expression": "creditScore" },
      { "name": "ltvRatio_v2",         "expression": "ltvRatio" },
      { "name": "creditAdjustment_v2", "expression": "creditAdjustment_v2" },
      { "name": "defaultRate_v2",      "expression": "defaultRate_v2" },
      { "name": "adjustedRate_v2",     "expression": "adjustedRate_v2" },
      { "name": "term_v2",             "expression": "term_v2" },
      { "name": "monthlyRate_v2",      "expression": "monthlyRate_v2" },
      { "name": "monthlyInterest_v2",  "expression": "monthlyInterest_v2" },
      { "name": "pmt_v2",              "expression": "pmt_v2" },
      { "name": "principal_v1",        "expression": "principal" },
      { "name": "annualRate_v1",       "expression": "annualRate_v1" },
      { "name": "term_v1",             "expression": "term_v1" },
      { "name": "monthlyRate_v1",      "expression": "monthlyRate_v1" },
      { "name": "monthlyInterest_v1",  "expression": "monthlyInterest_v1" },
      { "name": "pmt_v1",              "expression": "pmt_v1" }
    ]
  }
}'

# Execution Results:
{
    "content": [
        {
            "id": "d8795517-ff5c-4607-8ec9-03b8e6d74f45",
            "loanId": "LN001",
            "output": {
                "results": {
                    "pd_v3": 0.02,
                    "ead_v3": 100000,
                    "lgd_v3": 0.45,
                    "loanId": "LN001",
                    "pmt_v1": 45833.33333333333,
                    "pmt_v2": 50000,
                    "term_v1": 360,
                    "term_v2": 360,
                    "stage_v3": 1,
                    "baseECL_v3": 2.8107832384785554,
                    "finalECL_v3": 100,
                    "ltvRatio_v2": 0.025,
                    "principal_v1": 100000.0,
                    "principal_v2": 100000.0,
                    "annualRate_v1": 5.5,
                    "adjustedECL_v3": 3.3729398861742665,
                    "creditScore_v2": 650,
                    "defaultRate_v2": 0.02,
                    "monthlyRate_v1": 0.4583333333333333,
                    "monthlyRate_v2": 0.5,
                    "adjustedRate_v2": 6,
                    "timeToDefault_v3": 2.5,
                    "discountFactor_v3": 0.0031230924871983945,
                    "monthlyInterest_v1": 45833.33333333333,
                    "monthlyInterest_v2": 50000,
                    "stageMultiplier_v3": 1.0,
                    "creditAdjustment_v2": 0.5
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
                        "ltvRatio": "0.025",
                        "creditScore": "650",
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
                    "pmt_v1": 45833.33333333333,
                    "pmt_v2": 50000,
                    "baseECL": 2.8107832384785554,
                    "term_v1": 360,
                    "term_v2": 360,
                    "finalECL": 100,
                    "adjustedECL": 3.3729398861742665,
                    "ltvBucket_v2": "low",
                    "annualRate_v1": 5.5,
                    "annualRate_v2": 5.5,
                    "termBucket_v2": "long",
                    "defaultRate_v2": 0.02,
                    "discountFactor": 0.0031230924871983945,
                    "monthlyRate_v1": 0.4583333333333333,
                    "monthlyRate_v2": 0.5,
                    "adjustedRate_v2": 6,
                    "stageMultiplier": 1.0,
                    "monthlyInterest_v1": 45833.33333333333,
                    "monthlyInterest_v2": 50000,
                    "creditAdjustment_v2": 0.5
                }
            },
            "createdAt": "2025-09-26T19:02:40.73392"
        },
        {
            "id": "88dd2f33-c2e8-45af-9383-9aa8a6e3e3af",
            "loanId": "LN002",
            "output": {
                "results": {
                    "pd_v3": 0.05,
                    "ead_v3": 250000,
                    "lgd_v3": 0.6,
                    "loanId": "LN002",
                    "pmt_v1": 17500.0,
                    "pmt_v2": 16458.333333333332,
                    "term_v1": 180,
                    "term_v2": 180,
                    "stage_v3": 2,
                    "baseECL_v3": 117.8046280050042,
                    "finalECL_v3": 353,
                    "ltvRatio_v2": 0.04,
                    "principal_v1": 50000.0,
                    "principal_v2": 50000.0,
                    "annualRate_v1": 4.2,
                    "adjustedECL_v3": 353.4138840150126,
                    "creditScore_v2": 800,
                    "defaultRate_v2": 0.02,
                    "monthlyRate_v1": 0.35000000000000003,
                    "monthlyRate_v2": 0.32916666666666666,
                    "adjustedRate_v2": 3.95,
                    "timeToDefault_v3": 1.8,
                    "discountFactor_v3": 0.01570728373400056,
                    "monthlyInterest_v1": 17500.0,
                    "monthlyInterest_v2": 16458.333333333332,
                    "stageMultiplier_v3": 2.5,
                    "creditAdjustment_v2": -0.25
                },
                "positionData": {
                    "principal": 50000.0,
                    "termMonths": 180,
                    "customFields": {
                        "pd": "0.05",
                        "ead": "250000",
                        "lgd": "0.60",
                        "fico": "850",
                        "stage": "2",
                        "ltvRatio": "0.04",
                        "creditScore": "800",
                        "scoreOverride": "No",
                        "timeToDefault": "1.8",
                        "underwriterName": "Bob"
                    },
                    "interestRate": 4.2,
                    "rateSchedule": [
                        {
                            "rate": 4.2,
                            "effectiveDate": "2021-06-01"
                        }
                    ],
                    "originationDate": "2021-06-01",
                    "paymentSchedule": [
                        {
                            "endDate": "2021-07-01",
                            "startDate": "2021-07-01",
                            "paymentType": "Prepay",
                            "monthlyPayment": 3000.0,
                            "interestPayment": 1500.0,
                            "principalPayment": 1500.0
                        }
                    ],
                    "amortizationType": "Fixed"
                },
                "derivedAttributes": {
                    "pmt_v1": 17500.0,
                    "pmt_v2": 16458.333333333332,
                    "baseECL": 117.8046280050042,
                    "term_v1": 180,
                    "term_v2": 180,
                    "finalECL": 353,
                    "adjustedECL": 353.4138840150126,
                    "ltvBucket_v2": "low",
                    "annualRate_v1": 4.2,
                    "annualRate_v2": 4.2,
                    "termBucket_v2": "short",
                    "defaultRate_v2": 0.02,
                    "discountFactor": 0.01570728373400056,
                    "monthlyRate_v1": 0.35000000000000003,
                    "monthlyRate_v2": 0.32916666666666666,
                    "adjustedRate_v2": 3.95,
                    "stageMultiplier": 2.5,
                    "monthlyInterest_v1": 17500.0,
                    "monthlyInterest_v2": 16458.333333333332,
                    "creditAdjustment_v2": -0.25
                }
            },
            "createdAt": "2025-09-26T19:02:40.736004"
        },
        {
            "id": "d66fa036-7278-4143-a298-12796bab5794",
            "loanId": "LN003",
            "output": {
                "results": {
                    "pd_v3": 0.15,
                    "ead_v3": 75000,
                    "lgd_v3": 0.75,
                    "loanId": "LN003",
                    "pmt_v1": 221875.0,
                    "pmt_v2": 221875,
                    "term_v1": 360,
                    "term_v2": 360,
                    "stage_v3": 3,
                    "baseECL_v3": 2661.5262566665338,
                    "finalECL_v3": 15969,
                    "ltvRatio_v2": 0.03,
                    "principal_v1": 750000.0,
                    "principal_v2": 750000.0,
                    "annualRate_v1": 3.55,
                    "adjustedECL_v3": 15969.157539999203,
                    "creditScore_v2": 666,
                    "defaultRate_v2": 0.02,
                    "monthlyRate_v1": 0.29583333333333334,
                    "monthlyRate_v2": 0.29583333333333334,
                    "adjustedRate_v2": 3.55,
                    "timeToDefault_v3": 0.5,
                    "discountFactor_v3": 0.31544014893825584,
                    "monthlyInterest_v1": 221875.0,
                    "monthlyInterest_v2": 221875,
                    "stageMultiplier_v3": 5.0,
                    "creditAdjustment_v2": 0
                },
                "positionData": {
                    "principal": 750000.0,
                    "termMonths": 360,
                    "customFields": {
                        "pd": "0.15",
                        "ead": "75000",
                        "lgd": "0.75",
                        "fico": "600",
                        "stage": "3",
                        "ltvRatio": "0.03",
                        "creditScore": "666",
                        "scoreOverride": "Yes",
                        "timeToDefault": "0.5",
                        "underwriterName": "Joe"
                    },
                    "interestRate": 3.55,
                    "rateSchedule": [
                        {
                            "rate": 5.5,
                            "effectiveDate": "2020-01-01"
                        }
                    ],
                    "originationDate": "2021-06-01",
                    "paymentSchedule": [
                        {
                            "endDate": "2020-02-01",
                            "startDate": "2020-02-01",
                            "paymentType": "Scheduled",
                            "monthlyPayment": 5000.0,
                            "interestPayment": 1000.0,
                            "principalPayment": 4000.0
                        }
                    ],
                    "amortizationType": "Fixed"
                },
                "derivedAttributes": {
                    "pmt_v1": 221875.0,
                    "pmt_v2": 221875,
                    "baseECL": 2661.5262566665338,
                    "term_v1": 360,
                    "term_v2": 360,
                    "finalECL": 15969,
                    "adjustedECL": 15969.157539999203,
                    "ltvBucket_v2": "low",
                    "annualRate_v1": 3.55,
                    "annualRate_v2": 3.55,
                    "termBucket_v2": "long",
                    "defaultRate_v2": 0.02,
                    "discountFactor": 0.31544014893825584,
                    "monthlyRate_v1": 0.29583333333333334,
                    "monthlyRate_v2": 0.29583333333333334,
                    "adjustedRate_v2": 3.55,
                    "stageMultiplier": 5.0,
                    "monthlyInterest_v1": 221875.0,
                    "monthlyInterest_v2": 221875,
                    "creditAdjustment_v2": 0
                }
            },
            "createdAt": "2025-09-26T19:02:40.737527"
        },
        {
            "id": "68008e9f-d21c-45af-bce9-d7055257264b",
            "loanId": "LN004",
            "output": {
                "results": {
                    "pd_v3": 0.01,
                    "ead_v3": 500000,
                    "lgd_v3": 0.4,
                    "loanId": "LN004",
                    "pmt_v1": 2870.833333333333,
                    "pmt_v2": 2870.833333333333,
                    "term_v1": 180,
                    "term_v2": 180,
                    "stage_v3": 1,
                    "baseECL_v3": 1.241934232394595,
                    "finalECL_v3": 500,
                    "ltvRatio_v2": 0.09,
                    "principal_v1": 5000.0,
                    "principal_v2": 5000.0,
                    "annualRate_v1": 6.89,
                    "adjustedECL_v3": 1.490321078873514,
                    "creditScore_v2": 635,
                    "defaultRate_v2": 0.02,
                    "monthlyRate_v1": 0.5741666666666666,
                    "monthlyRate_v2": 0.5741666666666666,
                    "adjustedRate_v2": 6.89,
                    "timeToDefault_v3": 3.2,
                    "discountFactor_v3": 6.209671161972975E-4,
                    "monthlyInterest_v1": 2870.833333333333,
                    "monthlyInterest_v2": 2870.833333333333,
                    "stageMultiplier_v3": 1.0,
                    "creditAdjustment_v2": 0
                },
                "positionData": {
                    "principal": 5000.0,
                    "termMonths": 180,
                    "customFields": {
                        "pd": "0.01",
                        "ead": "500000",
                        "lgd": "0.40",
                        "fico": "650",
                        "stage": "1",
                        "ltvRatio": "0.09",
                        "creditScore": "635",
                        "scoreOverride": "No",
                        "timeToDefault": "3.2",
                        "underwriterName": "Tim"
                    },
                    "interestRate": 6.89,
                    "rateSchedule": [
                        {
                            "rate": 4.2,
                            "effectiveDate": "2021-06-01"
                        }
                    ],
                    "originationDate": "2021-06-01",
                    "paymentSchedule": [
                        {
                            "endDate": "2021-07-01",
                            "startDate": "2021-07-01",
                            "paymentType": "Prepay",
                            "monthlyPayment": 9000.0,
                            "interestPayment": 1500.0,
                            "principalPayment": 7500.0
                        }
                    ],
                    "amortizationType": "Fixed"
                },
                "derivedAttributes": {
                    "pmt_v1": 2870.833333333333,
                    "pmt_v2": 2870.833333333333,
                    "baseECL": 1.241934232394595,
                    "term_v1": 180,
                    "term_v2": 180,
                    "finalECL": 500,
                    "adjustedECL": 1.490321078873514,
                    "ltvBucket_v2": "low",
                    "annualRate_v1": 6.89,
                    "annualRate_v2": 6.89,
                    "termBucket_v2": "short",
                    "defaultRate_v2": 0.02,
                    "discountFactor": 6.209671161972975E-4,
                    "monthlyRate_v1": 0.5741666666666666,
                    "monthlyRate_v2": 0.5741666666666666,
                    "adjustedRate_v2": 6.89,
                    "stageMultiplier": 1.0,
                    "monthlyInterest_v1": 2870.833333333333,
                    "monthlyInterest_v2": 2870.833333333333,
                    "creditAdjustment_v2": 0
                }
            },
            "createdAt": "2025-09-26T19:02:40.739222"
        },
        {
            "id": "6d28ef11-7472-4c97-8335-6d3c833f997a",
            "loanId": "LN005",
            "output": {
                "results": {
                    "pd_v3": 0.08,
                    "ead_v3": 180000,
                    "lgd_v3": 0.55,
                    "loanId": "LN005",
                    "pmt_v1": 74250.0,
                    "pmt_v2": 74250,
                    "term_v1": 360,
                    "term_v2": 360,
                    "stage_v3": 2,
                    "baseECL_v3": 496.7363107374433,
                    "finalECL_v3": 1490,
                    "ltvRatio_v2": 0.02,
                    "principal_v1": 90000.0,
                    "principal_v2": 90000.0,
                    "annualRate_v1": 9.9,
                    "adjustedECL_v3": 1490.20893221233,
                    "creditScore_v2": 680,
                    "defaultRate_v2": 0.02,
                    "monthlyRate_v1": 0.8250000000000001,
                    "monthlyRate_v2": 0.8250000000000001,
                    "adjustedRate_v2": 9.9,
                    "timeToDefault_v3": 1.2,
                    "discountFactor_v3": 0.06271923115371758,
                    "monthlyInterest_v1": 74250.0,
                    "monthlyInterest_v2": 74250,
                    "stageMultiplier_v3": 2.5,
                    "creditAdjustment_v2": 0
                },
                "positionData": {
                    "principal": 90000.0,
                    "termMonths": 360,
                    "customFields": {
                        "pd": "0.08",
                        "ead": "180000",
                        "lgd": "0.55",
                        "fico": "680",
                        "stage": "2",
                        "ltvRatio": "0.02",
                        "creditScore": "680",
                        "scoreOverride": "Yes",
                        "timeToDefault": "1.2",
                        "underwriterName": "Will"
                    },
                    "interestRate": 9.9,
                    "rateSchedule": [
                        {
                            "rate": 5.5,
                            "effectiveDate": "2020-01-01"
                        }
                    ],
                    "originationDate": "2021-06-01",
                    "paymentSchedule": [
                        {
                            "endDate": "2020-02-01",
                            "startDate": "2020-02-01",
                            "paymentType": "Scheduled",
                            "monthlyPayment": 3300.0,
                            "interestPayment": 1000.0,
                            "principalPayment": 2300.0
                        }
                    ],
                    "amortizationType": "Fixed"
                },
                "derivedAttributes": {
                    "pmt_v1": 74250.0,
                    "pmt_v2": 74250,
                    "baseECL": 496.7363107374433,
                    "term_v1": 360,
                    "term_v2": 360,
                    "finalECL": 1490,
                    "adjustedECL": 1490.20893221233,
                    "ltvBucket_v2": "low",
                    "annualRate_v1": 9.9,
                    "annualRate_v2": 9.9,
                    "termBucket_v2": "long",
                    "defaultRate_v2": 0.02,
                    "discountFactor": 0.06271923115371758,
                    "monthlyRate_v1": 0.8250000000000001,
                    "monthlyRate_v2": 0.8250000000000001,
                    "adjustedRate_v2": 9.9,
                    "stageMultiplier": 2.5,
                    "monthlyInterest_v1": 74250.0,
                    "monthlyInterest_v2": 74250,
                    "creditAdjustment_v2": 0
                }
            },
            "createdAt": "2025-09-26T19:02:40.740629"
        },
        {
            "id": "a6f8aef5-8d09-4976-85a3-be2d00bbea77",
            "loanId": "LN006",
            "output": {
                "results": {
                    "pd_v3": 0.25,
                    "ead_v3": 45000,
                    "lgd_v3": 0.8,
                    "loanId": "LN006",
                    "pmt_v1": 16666.666666666693,
                    "pmt_v2": 16666.666666666693,
                    "term_v1": 180,
                    "term_v2": 180,
                    "stage_v3": 3,
                    "baseECL_v3": 1420.7238137403508,
                    "finalECL_v3": 8524,
                    "ltvRatio_v2": 0.01,
                    "principal_v1": 80000.0,
                    "principal_v2": 80000.0,
                    "annualRate_v1": 2.5,
                    "adjustedECL_v3": 8524.342882442104,
                    "creditScore_v2": 690,
                    "defaultRate_v2": 0.02,
                    "monthlyRate_v1": 0.20833333333333334,
                    "monthlyRate_v2": 0.20833333333333334,
                    "adjustedRate_v2": 2.5,
                    "timeToDefault_v3": 0.8,
                    "discountFactor_v3": 0.15785820152670565,
                    "monthlyInterest_v1": 16666.666666666668,
                    "monthlyInterest_v2": 16666.666666666668,
                    "stageMultiplier_v3": 5.0,
                    "creditAdjustment_v2": 0
                },
                "positionData": {
                    "principal": 80000.0,
                    "termMonths": 180,
                    "customFields": {
                        "pd": "0.25",
                        "ead": "45000",
                        "lgd": "0.80",
                        "fico": "632",
                        "stage": "3",
                        "ltvRatio": "0.01",
                        "creditScore": "690",
                        "scoreOverride": "No",
                        "timeToDefault": "0.8",
                        "underwriterName": "Peter"
                    },
                    "interestRate": 2.5,
                    "rateSchedule": [
                        {
                            "rate": 4.2,
                            "effectiveDate": "2021-06-01"
                        }
                    ],
                    "originationDate": "2021-06-01",
                    "paymentSchedule": [
                        {
                            "endDate": "2021-07-01",
                            "startDate": "2021-07-01",
                            "paymentType": "Prepay",
                            "monthlyPayment": 3000.0,
                            "interestPayment": 1500.0,
                            "principalPayment": 1500.0
                        }
                    ],
                    "amortizationType": "Fixed"
                },
                "derivedAttributes": {
                    "pmt_v1": 16666.666666666693,
                    "pmt_v2": 16666.666666666693,
                    "baseECL": 1420.7238137403508,
                    "term_v1": 180,
                    "term_v2": 180,
                    "finalECL": 8524,
                    "adjustedECL": 8524.342882442104,
                    "ltvBucket_v2": "low",
                    "annualRate_v1": 2.5,
                    "annualRate_v2": 2.5,
                    "termBucket_v2": "short",
                    "defaultRate_v2": 0.02,
                    "discountFactor": 0.15785820152670565,
                    "monthlyRate_v1": 0.20833333333333334,
                    "monthlyRate_v2": 0.20833333333333334,
                    "adjustedRate_v2": 2.5,
                    "stageMultiplier": 5.0,
                    "monthlyInterest_v1": 16666.666666666668,
                    "monthlyInterest_v2": 16666.666666666668,
                    "creditAdjustment_v2": 0
                }
            },
            "createdAt": "2025-09-26T19:02:40.742254"
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
        "unpaged": false,
        "paged": true
    },
    "last": true,
    "totalPages": 1,
    "totalElements": 6,
    "first": true,
    "size": 50,
    "number": 0,
    "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
    },
    "numberOfElements": 6,
    "empty": false
}