# Configuration file for the Sphinx documentation builder.
#
# This file only contains a selection of the most common options. For a full
# list see the documentation:
# https://www.sphinx-doc.org/en/master/usage/configuration.html

# -- Path setup --------------------------------------------------------------

# If extensions (or modules to document with autodoc) are in another directory,
# add these directories to sys.path here. If the directory is relative to the
# documentation root, use os.path.abspath to make it absolute, like shown here.
#
import os
import sys

# Path to python sources, for doc generation on readthedocs
source_path = os.path.abspath('..')
sys.path.insert(0, source_path)
print(f'appended {source_path}')


# -- Project information -----------------------------------------------------

project = 'powsybl-openrao'
copyright = '2024, RTE (http://www.rte-france.com)'


# -- General configuration ---------------------------------------------------

# Add any Sphinx extension module names here, as strings. They can be
# extensions coming with Sphinx (named 'sphinx.ext.*') or your custom
# ones.
extensions = ['sphinx.ext.autodoc',
              'sphinx.ext.autosummary',
              'sphinx.ext.viewcode',
              'sphinx.ext.doctest',
              'sphinx.ext.napoleon',
              'sphinx.ext.todo',
              'sphinx.ext.intersphinx']

# Add any paths that contain templates here, relative to this directory.
templates_path = ['_templates']

# List of patterns, relative to source directory, that match files and
# directories to ignore when looking for source files.
# This pattern also affects html_static_path and html_extra_path.
exclude_patterns = ['_build', 'Thumbs.db', '.DS_Store']


# -- Options for HTML output -------------------------------------------------

# The theme to use for HTML and HTML Help pages.  See the documentation for
# a list of builtin themes.
#
html_theme = "pydata_sphinx_theme"

html_title = 'powsybl-openrao'
html_short_title = 'powsybl-openrao'

html_logo = '_static/logos/powsybl_logo.svg'
html_favicon = "_static/favicon.ico"

html_theme_options = {
    "icon_links": [
        {
            "name": "GitHub",
            "url": "https://github.com/powsybl/powsybl-open-rao",
            "icon": "fab fa-github-square",
        }
    ],
    "navbar_start": ["navbar-brand-powsybl-openrao"],
}

# Add any paths that contain custom static files (such as style sheets) here,
# relative to this directory. They are copied after the builtin static files,
# so a file named "default.css" will overwrite the builtin "default.css".
html_static_path = ['_static']

todo_include_todos = True

# Links to external documentations : python 3 and pandas
intersphinx_mapping = {
    'python': ('https://docs.python.org/3', None),
    'pandas': ('https://pandas.pydata.org/docs', None),
}

# Generate one file per method
autosummary_generate = True