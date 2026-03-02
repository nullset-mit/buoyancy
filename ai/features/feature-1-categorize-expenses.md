Feature 1
* Feature description
A command line utility that provides a way to categorize expenses
* Feature requirements
1. A command line utility
2. Feature will be written in Java
3. Create the "Expense Categorization Workflow" and all sub-workflows
** Expense Categorization Workflow Steps
1. User provides a csv with expenses in a given format
2. Application parses csv into Transaction objects
3. Each Transaction object has an Id (UUID) Date (date), a Memo (string), a Counterparty (Object), an Amount (double), a Category (Object), and a Sub-Category
4. A Counterparty has an Id (UUID) and a Name (String)
5. A Category has an Id (UUID), a Name (String), and an optional Sub-Category (String)
6. The transactions are then saved into a local database.
7. To find and parse the Transactions, the application will use Categorization Rules
8. A categorization rule looks at the Memo and the Amount and determines what Counterparty and Category to assign to the transaction.
9. If no matching rules are found, the user is prompted to create a new categorization rule. This begins the Categorization Rule Creation Workflow
10. If the user wishes to dismiss this line, he may type 'skip' to skip.
11. This process repeats until all lines are categorized or skipped.
** Categorization Rule Creation Workflow
1. User creates a string with wildcards such as '%' that will match this memo.
2. User selects whether this pattern should apply to credits, debits, or both
3. User then assigns a Counterparty to this rule, or if a suitable one does not exist, creates one using the Counterparty Creation Workflow
4. User then assigns a Category to this rule, or if a suitable one does not exist, creates one using the Category Creation Workflow
5. User then assigns a Sub-Category to this rule, or if a suitable one does not exist, creates one using the Sub-Category Creation Workflow
6. The rule is then saved to the database.
*** Counterparty Creation Workflow
1. User enters the Name of the Counterparty
2. Counterparty is saved to the database
*** Category Creation Workflow
1. User enters the Name of the Category
2. Category is saved to the database
*** Sub-Category Creation Workflow
1. User enters the Name of the Sub-Category
2. Sub-Category is saved to the database

* Implementation Phases
** [COMPLETE] Phase 1 - Hello World
1. Create src/java/ directory to store source code
2. Create command-line entrypoint to the application under the net.mossworks.buoyancy package
3. This main class will just have a simple "hello world" message as a placeholder.
4. Set up a maven project to build the application
5. Add to the .gitignore files relevant to emacs, such as files ending in ~ and #

** [COMPLETE] Phase 2 - Domain Classes
1. Create the Counterparty class inside the net.mossworks.buoyancy.domain package, to the specification in the feature description
2. Create the Category class inside the same domain package, each to the specification in the feature description
3. Create the Transaction classes inside the same domain package.
4. In the Transaction class, add validation to the constructor requiring all fields to exist, and validating that the amount is numeric and does not contain decimal places beyond .01

** [COMPLETE] Phase 3 - Application Classes and Classifier Interface
1. Create a new package: net.mossworks.buoyancy.application.dto
2. Create a new class inside this package, UnclassifiedTransaction, which has three fields:
   - a memo (String)
   - an amount (double)
   - a Date
   Each of these are required, and the amount should not have a decimal with higher precision than .01
3. Create a new class in the domain package called ClassificationRule, which has the following properties:
   - a pattern string called memoPattern
   - an integer priority, with 0 being the default
   - a Category object called category
   - an Enum for 'CREDIT', 'DEBIT', or 'BOTH', depending on whether this rule applies to credits, debits, or both.
   - an Id (UUID)
4. Create a new abstract TransactionClassifier class in the net.mossworks.buoyancy.application package. This will be the abstract base class for anything that classifies transactions.
   This abstract class should have a public method classify that takes in an UnclassifiedTransaction and outputs a Counterparty.
5. Create a new class RuleBasedTransactionClassifier which extends TransactionClassifier.
   This classifier should have a private List of type ClassificationRule which are passed in as part of the constructor.
   The implementation for classify should initially be blank.

** [COMPLETE] Phase 4 - Transaction Classification Rule Repository
✓ 1. Create a new interface in the net.mossworks.buoyancy.application.repository package called ClassificationRuleRepository.java. This interface defines a method called loadRules that takes no arguments and returns a List of ClassificationRule objects
✓ 2. Create a setter for rules inside RuleBasedTransactionClassifier
✓ 3. Add ClassificationRuleRepository as a required argument to RuleBasedTransactionClassifier, and create a method loadRules that gets the rules from the repository.
✓ 4. Create YamlFileClassificationRuleRepository as an implementation of ClassificationRuleRepository, which reads and writes from a file.
   The format of these files will be yaml, and the spec for each rule should match ClassificationRule.java
   This class should have a mandatory FilePath attribute that determines where the rule I/O goes to.
✓ 5. Implement a writeRule method on this class that writes a given rule to the output file.
✓ 6. Write unit tests for this new class.

** Phase 5 - Connect the implementation of the classifier to the implementation of the repository
<vague>wire this to the command line</vague>
<vague>create an integration test for the tool</vague>

** Phase 6 - Documentation
<vague>create a mkdocs.yaml file, and a docs directory</vague>
<vague>create a clean architecture / hexagonal diagram of the system [META] Try to improve your skills at AI-assisted documentation</vague>

