# Add Assumption Set
curl --location 'http://localhost:8081/api/assumptions' \
--form 'name="AssumptionSetV2"' \
--form 'description="Baseline scenario for expected cashflows"' \
--form 'base_rate="0.99"' \
--form 'prepayment_enabled="false"' \
--form 'stress_factor="1.10"' \
--form 'credit_adjustments=@"/Users/NomanAhmed/Documents/Noman/code/github/risk-valuation-platform/test-data/assumptions/v2/credit_adjustments.csv"' \
--form 'default_matrix=@"/Users/NomanAhmed/Documents/Noman/code/github/risk-valuation-platform/test-data/assumptions/v2/default_matrix.csv"' \
--form 'base_annual_rate="0.08"' \
--form 'default_term_months="120"'

# Add Position File
curl --location 'http://localhost:8080/api/positions/upload' \
--form 'name="V2"' \
--form 'positionDate="2025-11-30"' \
--form 'file=@"/Users/NomanAhmed/Documents/Noman/code/github/risk-valuation-platform/test-data/positionfiles/v2/PositionFile_11302025.zip"'

# Add Model
curl --location 'http://localhost:8082/api/models' \
--header 'Content-Type: application/json' \
--data '{
    "name": "Working Model v2 With Tables Lookup",
    "description": "Working Model v2 with clean assumption.lookup syntax",
    "modelDefinition": {
      "inputs": {
        "positionFields": ["loanNumber","principal","interestRate","termMonths","originationDate","creditScore","ltvRatio"],
        "assumptionValues": ["base_annual_rate","default_term_months"],
        "csvTables": ["credit_adjustments","default_matrix"]
      },
      "derivedFields": [
        {
          "name": "annualRate",
          "expression": "(interestRate != null) ? interestRate : 5.0"
        },
        {
          "name": "term",
          "expression": "(termMonths != null) ? termMonths : 360"
        },
        {
          "name": "creditAdjustment",
          "expression": "fn.toNumber(assumption.tableLookup.lookup('\''credit_adjustments'\'', '\''credit_score'\'', creditScore, '\''rate_adjustment'\''))"
        },
        {
          "name": "ltvBucket",
          "expression": "(ltvRatio <= 0.8) ? '\''low'\'' : (ltvRatio <= 0.9) ? '\''medium'\'' : '\''high'\''"
        },
        {
          "name": "termBucket",
          "expression": "(term <= 180) ? '\''short'\'' : (term <= 300) ? '\''medium'\'' : '\''long'\''"
        },
        {
          "name": "defaultRate",
          "expression": "fn.toNumber(assumption.tableLookup.lookup('\''default_matrix'\'', '\''ltv_bucket'\'', ltvBucket, '\''long_term'\''))"
        },
        {
          "name": "adjustedRate",
          "expression": "annualRate + (creditAdjustment != null ? creditAdjustment : 0)"
        },
        {
          "name": "monthlyRate",
          "expression": "adjustedRate / 12"
        },
        {
          "name": "monthlyInterest",
          "expression": "principal * monthlyRate"
        },
        {
          "name": "pmt",
          "expression": "(term != null && monthlyRate != null && monthlyRate > 0) ? (principal * monthlyRate / (1 - Math.pow(1 + monthlyRate, -term))) : null"
        }
      ],
      "outputs": [
        { "name": "loanNumber",       "expression": "loanNumber" },
        { "name": "principal",        "expression": "principal" },
        { "name": "creditScore",      "expression": "creditScore" },
        { "name": "ltvRatio",         "expression": "ltvRatio" },
        { "name": "creditAdjustment", "expression": "creditAdjustment" },
        { "name": "defaultRate",      "expression": "defaultRate" },
        { "name": "adjustedRate",     "expression": "adjustedRate" },
        { "name": "term",             "expression": "term" },
        { "name": "monthlyRate",      "expression": "monthlyRate" },
        { "name": "monthlyInterest",  "expression": "monthlyInterest" },
        { "name": "pmt",              "expression": "pmt" }
      ]
    }
  }'

# Execution Results:
{
    "content": [
        {
            "id": "21c6c2e8-3276-4a56-9e46-9ec1d65a2ccc",
            "loanId": "LN001",
            "output": {
                "results": {
                    "pmt": 50000,
                    "term": 360,
                    "ltvRatio": 0.025,
                    "principal": 100000.0,
                    "loanNumber": "LN001",
                    "creditScore": 650,
                    "defaultRate": 0.02,
                    "monthlyRate": 0.5,
                    "adjustedRate": 6,
                    "monthlyInterest": 50000,
                    "creditAdjustment": 0.5
                },
                "positionData": {
                    "principal": 100000.0,
                    "termMonths": 360,
                    "customFields": {
                        "fico": "700",
                        "ltvRatio": "0.025",
                        "creditScore": "650",
                        "scoreOverride": "Yes",
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
                    "pmt": 50000,
                    "term": 360,
                    "ltvBucket": "low",
                    "annualRate": 5.5,
                    "termBucket": "long",
                    "defaultRate": 0.02,
                    "monthlyRate": 0.5,
                    "adjustedRate": 6,
                    "monthlyInterest": 50000,
                    "creditAdjustment": 0.5
                }
            },
            "createdAt": "2025-09-26T18:32:27.051883"
        },
        {
            "id": "1112199a-5787-4c83-ae7c-76dde1f878e8",
            "loanId": "LN002",
            "output": {
                "results": {
                    "pmt": 18541.666666666668,
                    "term": 180,
                    "ltvRatio": 0.04,
                    "principal": 50000.0,
                    "loanNumber": "LN002",
                    "creditScore": 700,
                    "defaultRate": 0.02,
                    "monthlyRate": 0.37083333333333335,
                    "adjustedRate": 4.45,
                    "monthlyInterest": 18541.666666666668,
                    "creditAdjustment": 0.25
                },
                "positionData": {
                    "principal": 50000.0,
                    "termMonths": 180,
                    "customFields": {
                        "fico": "850",
                        "ltvRatio": "0.04",
                        "creditScore": "700",
                        "scoreOverride": "No",
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
                            "paymentType": "Scheduled",
                            "monthlyPayment": 3000.0,
                            "interestPayment": 1500.0,
                            "principalPayment": 1500.0
                        }
                    ],
                    "amortizationType": "Fixed"
                },
                "derivedAttributes": {
                    "pmt": 18541.666666666668,
                    "term": 180,
                    "ltvBucket": "low",
                    "annualRate": 4.2,
                    "termBucket": "short",
                    "defaultRate": 0.02,
                    "monthlyRate": 0.37083333333333335,
                    "adjustedRate": 4.45,
                    "monthlyInterest": 18541.666666666668,
                    "creditAdjustment": 0.25
                }
            },
            "createdAt": "2025-09-26T18:32:27.054044"
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
    "totalElements": 2,
    "first": true,
    "size": 50,
    "number": 0,
    "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
    },
    "numberOfElements": 2,
    "empty": false
}