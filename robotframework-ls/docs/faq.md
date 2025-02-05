Robot Framework Language Server FAQ
======================================

How to proceed if Keywords from a library are not being resolved?
------------------------------------------------------------------

The most common reason this happens is because some error happened when trying to automatically generate the libspec for a given library.

If you're in `VSCode`, the error may be seen in the `OUTPUT` tab, selecting `Robot Framework` in the dropdown.

For `IntelliJ`, it's possible to see it by turning on the logging (see [Reporting Issues](reporting_issues.md) for details -- look for `EXCEPTION` in the log files).

**Important**: After the logs are collected, revert the changes to stop the logging (having the logging on may make the language server go slower).

The common errors here are:

**1. The library requires arguments to be initialized**
In this case there are 3 different approaches that can be used:

**a.** Change the library to have default arguments so that libspec can generate it out of the box.
  After the library is changed to accept default arguments, you may need to restart your editor/IDE to clear the related caches.

**b.** Use the `robot.libraries.libdoc.needsArgs` to specify that the arguments typed in the editor should be passed to the library to initialize it.

**Example**: If you have a declaration such as:

```RobotFramework
*** Settings ***
Library   My Library  arg1  arg2
```

It's possible to set `"robot.libraries.libdoc.needsArgs": ["My Library"]`.

In which case the libspec will be generated by passing `arg1` and `arg2` to initialize the library.

Note that in such circumstances, if the library is used from different places with different arguments each place will generate a different libspec (as different arguments could result in different keywords.

**c.** Manually generate the libspec for the library and put it somewhere in your workspace or right below a folder in the `PYTHONPATH`.
It is possible to manually generate a libspec by executing something like:

`python -m robot.libdoc <library_name> <library_name.libspec>`

**Notes**:

- `-a` may be used to specify an argument
- In RobotFramework 3.x `--format XML:HTML` must also be passed
- See `python -m robot.libdoc -h` for more details

Whenever the library changes, make sure you manually regenerate the libspec.

**2. The library is not present in the python executable you're using.**

In this case, install the library in the given python executable or choose a different python executable which has the
library installed.

After the library is installed, you may need to restart your editor/IDE to clear the related caches.

**3. The library requires runtime information to be imported.**

In this case, please change the library so that it doesn't need runtime information to be imported.
i.e.: generating the `.libspec` as `python -m robot.libdoc <library_name><library_name.libspec>` requires the library to be imported. If it cannot be imported it's not possible to generate its libspec (and thus the language server cannot collect its information).

After the library is changed, you may need to restart your editor/IDE to clear the related caches.

How to deal with undefined variables?
---------------------------------------

Since `Robot Framework Language Server` `0.43.0`, usages of undefined variables are detected and reported 
during the linting.

The analysis of undefined variables is far from trivial as `Robot Framework`
itself has many ways of specifying variables, including dynamic variables which are very hard to
detect during the static analysis (which could result in false positives -- i.e.:
cases where a variable is reported as undefined when in reality it isn't).

As such, it's possible to tweak the analysis for the cases where a false positive is found by using 
the following approaches:

**1. Statically define a variable using the settings**

It's possible to specify a variable using the `robot.variables`. Variables defined through this setting
are also automatically passed as arguments to `Robot Framework`.

i.e.: A definition such as: 

```json
"robot.variables": { "myfolder": "${workspaceFolder}/myfolder" },
```  

Will have the effect of automatically passing `-v myfolder:/full/path/to/myfolder` to any
launches and the `myfolder` variable will also be detected during the linting.

**2. Ignore a variable during the linting using the settings**

It's possible to ignore the usage of specific variables during linting (this is
the preferred approach when the variable is dynamically assigned and the static
analyzer can't infer the variable creation).

i.e.: A definition such as:

```json
"robot.lint.ignoreVariables": ["var1", "var2"]
```

Will have the effect of not reporting as undefined any usage of `var1` or `var2`.

**3. Load variables defined in an arguments file**

If variables are usually defined in an arguments file, it's possible to ask
the language server to load those values from the arguments file (this is
very similar to specifying the variable in the first case where we specify
`robot.variables`, but it may be interesting to use in cases where variables
are already passed using arguments files (note that the arguments files still
have to be manually assigned in the launch configuration using the `args` in the launch).

i.e.: A definition such as:

```json
"robot.loadVariablesFromArgumentsFile": "${workspaceFolder}/arguments.txt"
```

Will have the effect of loading all `-v var:name` and `--variable var:name` definitions
from the arguments file.

**4. Disable the undefined variables lint**

Even though a lot of effort went into the analysis of undefined variables, it's
possible that you may hit some use-case which is not well supported (for instance,
right now it's known that not all variables from python files are correctly loaded, 
especially cases where variables are defined dynamically as the only support right 
now is statically analyzing python files to load variables).

In such cases it's possible to disable the analysis of undefined variables altogether.


i.e.: A definition such as:

```json
"robot.lint.variables": false
```

will disable the linting of undefined variables.


**5. Report case which should work as bug**

If you have a case where you believe the language server should detect
properly and it's not detecting (and you're having to work around it by disabling
linting or specifying a variable to be ignored), please create an issue in:

https://github.com/robocorp/robotframework-lsp/issues

So that it can be fixed in the future ;)



How to specify a variable needed to resolve some library or resource import?
-----------------------------------------------------------------------------

In this case, the path needed to resolve the variable needs to be specified in a 
variable in the `robot.variables` setting.

Note that relative paths aren't supported, but it's possible to use the following variables in the variable value:

- `${workspace}`
- `${workspaceRoot}`
- `${workspaceFolder}`
- `${env.ENV_NAME_VARIABLE}`
- `${env:ENV_NAME_VARIABLE}`

**Example**: Given a resource import such as: `Resource  ${ROOT}/my.resource`, `robot.variables` could be set to something like:

- `{"ROOT": "c:/my/project/src"}`
- `{"ROOT": "${workspaceRoot}/src"}`
- `{"ROOT": "${env:MY_ROOT}/src"}`

How to enable/disable autoformat on save on VSCode?
---------------------------------------------------

1. Open command palette (`Ctrl+Shift+P`) and select the command `Preferences: Configure Language Specific Settings...`.
2. Select `Robot Framework (robotframework)`
3. After the `settings.json` is opened, type in:
   ```json
   "[robotframework]": {
     "editor.formatOnSave": true
   },
   ```

How to configure the Robocop linter?
------------------------------------

To configure the linter create a `.robocop` file in your workspace root and fill it with the values you want.
Note: a `.robocop` file is a file with command line options to configure `Robocop`, see: https://robocop.readthedocs.io/en/latest/user_guide.html for details on the available command line options.

**Example** of `.robocop` file:

```ini
--exclude missing-doc-testcase
--exclude missing-doc-suite
```

How to enable the Robocop linter?
---------------------------------------

To enable the `Robocop` linter, change the setting:
`robot.lint.robocop.enabled`
to `true` (in the IntelliJ UI, it's the `Lint Robocop Enabled` setting).

How to change the Robocop version used?
---------------------------------------

The language server will initially try to load the version available from the `robot.python.executable` that's being used (which defaults to the same version used to start up the language server itself), so, if you want to use a different `Robocop` version, just install it so that it's importable in the proper Python environment (note: the minimun `Robocop` version is `1.6.1`).

**Note** that a default version is also shipped (but it may not be the latest `Robocop` version).

How to install a build from GitHub on IntelliJ?
-----------------------------------------------

First download the `IntelliJ-distribution.zip` from one of the [Tests IntelliJ](https://github.com/robocorp/robotframework-lsp/actions?query=workflow%3A%22Tests+-+IntelliJ%22) jobs in [https://github.com/robocorp/robotframework-lsp/actions](https://github.com/robocorp/robotframework-lsp/actions), then extract the `robotframework-IntelliJ-X.XX.X.zip` from it (due to a limitation in the GitHub upload artifacts action, even a single .zip is zipped again).

Afterwards, proceed to `File` > `Settings` > `Plugins`, click the ⚙️ 'gear' icon, choose `Install Plugin from Disk...`, point to the `robotframework-IntelliJ-X.XX.X.zip` file and then restart IntelliJ.

**Note** (common on macOS): if you unzipped and instead of the `robotframework-IntelliJ-X.XX.X.zip` you get directories, your .zip program is automatically unzipping the .zip inside the `distribution-IntelliJ.zip`. In this case you can either use a different program to unzip or re-zip those extracted contents into a new .zip.

How to change the file-watch mode?
----------------------------------

By default the language server uses `watchdog` for native file watching on Windows and polling (through `fsnotify`) on Mac and Linux (because for these using the `watchdog` library may run out of system resources, in which case those limits may have to be manually raised).

It's possible to change the file-watch mode by setting an environment variable:
`ROBOTFRAMEWORK_LS_WATCH_IMPL` to one of the following values:

- `watchdog`: for native file watching (in this case, please also install the latest `watchdog` in your python environment and raise the related limits according to your workspace contents (see: https://pythonhosted.org/watchdog/installation.html for more details).
- `fsnotify` for file watching using polling.

After setting the environment variable on your system, please restart the language server client you're using so that it picks up the new environment variable value.

**Note**: when possible using `watchdog` is recommended.

**Note**: when using `fsnotify` mode, it's possible to specify directories to be ignored with an environment variable `ROBOTFRAMEWORK_LS_IGNORE_DIRS` which points to a JSON list with glob-patterns to ignore.

e.g.: `ROBOTFRAMEWORK_LS_IGNORE_DIRS=["**/bin", "**/other/project"]`
  **Note**: The following patterns are always ignored:
  `["**/.git", "**/__pycache__", "**/.idea", "**/node_modules", "**/.metadata", "**/.vscode"]`

How to solve (NO_ROBOT) too old for linting?
--------------------------------------------

This means that the Python which is being used doesn't have `Robot Framework` installed.
To fix this, please use a configure a Python executable which does have `Robot Framework` installed (either through `robot.language-server.python` or `robot.python.executable` 
-- see: [config.md](config.md) for details) or install `Robot Framework` in the Python that's being used by the language server.

How to configure the launch from a code lens/shortcut?
------------------------------------------------------

To configure the launch from a code lens or shortcut (such as the test run in the gutter), please create a launch
configuration named `Robot Framework: Launch template`.
i.e.: To configure the terminal to be an `integrated` terminal on all launches
and to specify all launches to have an additional `--argumentfile /path/to/arguments.txt`, it's possible to create a `.vscode/launch.json` such as:

```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "robotframework-lsp",
            "name": "Robot Framework: Launch template",
            "request": "launch",
            "terminal": "integrated",
            "args": ["--argumentfile", "/path/to/arguments.txt"]
        }
    ]
}
```

**Note**: alternatively it's also possible to create the `launch` configuration in the user or workspace-settings by creating an entry such as the one below
in (user or workspace) `settings.json`.

-- **Note**: if a `.vscode/launch.json` is present in the workspace it will take precedence over launch configuration in the workspace settings, even if it's empty (so, in this case you need to define it either in your user settings or and put all `launch` settings in `.vscode/settings.json` and erase your workspace `.vscode/launch.json`).

```json
"launch": {
    "configurations": [
        {
            "type": "robotframework-lsp",
            "name": "Robot Framework: Launch Template",
            "request": "launch",
            "terminal": "integrated",
            "args": ["--argumentfile", "/path/to/arguments.txt"]
        }
    ],
}
```

How to use variables in settings?
----------------------------------

Since `Robot Framework Language Server 0.17.0`, the settings may contain variables
in the settings.

The variables available are `${workspaceFolder}`, which points to the first opened folder or `${env:ENV_VAR_NAME}`, which will obtain the `ENV_VAR_NAME` from the environment variables.

Also, since `0.17.0`, it's also possible to prefix a setting value with `~` so that
the user homedir is replaced.

**Example**:

```json
{
  "robot.pythonpath": [
      "~/lib/", 
      "${workspaceFolder}/lib", 
      "${env:MYROOT}/lib"
    ]
}
```

Changes in `0.20.0`:

- `${workspaceFolder}` was added and is recommended (`${workspace}` and `${workspaceRoot}` are still kept as aliases).
- The `${env:ENV_VAR_NAME}` was added and is recommended over the `${env.ENV_VAR_NAME}` format (which was available since `0.17.0`).

How to debug high-cpu usage in Robot Framework Language Server?
---------------------------------------------------------------

Note: `Robot Framework Language Server 0.17.0` has a bugfix for a case which could result in high-cpu usage, so, make sure you have the newest version
released prior to reporting an issue.

If even after upgrading you have a Python process with high-cpu related to the `Robot Framework Language Server`, please create an issue with the related
`pstats` files following the steps provided in https://github.com/robocorp/robotframework-lsp/issues/350#issuecomment-842506969 

How to use the Interactive Console?
-----------------------------------

**Note**: only available for VSCode.

The `Interactive Console` may be started using the `Robot Framework: Start Interactive Console` action.

Note that its scope will be based on the currently opened `.robot` or `.resource` file (if a `.robot` or `.resource` is not opened, the `Interactive Console` will not be opened).

Alternatively it's also possible to start the `Interactive Console` and send the contents of a given section to it using the `Run in Interactive Console` code-lens, which should automatically open it and send the initial contents for execution.
-- **Note**: to enable code-lenses, the setting `robot.codeLens.enable` must be set to `true`. 

When the `Interactive Console` is opened, an initial task will be considered running. At this point, it's possible to either send full sections (such as `*** Settings ***` or `*** Keyword ***`) or evaluate Keywords line by line.
-- **Note**: it's also possible to send the contents of `*** Test Case ***` or `*** Task ***`, but a new task won't actually be started, instead, the keyword calls in this case will be executed as a block in the context of the `Test/Task` that is already running.
-- **Note**: to add a library/resource import it must be sent along with the `*** Settings ***` header.
-- **Note**: To print some variable, use the `Log` keyword with `console=True`. i.e.: `Log    ${var}    console=True`


How can I use a different separator
------------------------------------

By default the `Robot Framework Language Server` will use 4 spaces as the separator
between arguments, but it's possible to use a different separator (such as tabs or 2 spaces).

To customize this the setting:

`"robot.completions.keywords.argumentsSeparator"` must be set to the wanted value
so that the completions that automatically add arguments use the required separator
(for `tabs` use `\t` as the value).

Afterwards, the client itself needs to be set independently to handle tab to add
the proper amount of spacing (or tabs).

In VSCode this requires setting `"robot.editor.4spacesTab"` to `false` besides
adjusting other editor-related settings.

