<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="html" indent="yes"/>
    <xsl:decimal-format decimal-separator="." grouping-separator=","/>

    <xsl:key name="files" match="file" use="@name"/>

    <!-- Checkstyle XML Style Sheet by Rolf Wojtech <rolf@wojtech.de>                   -->
    <!-- (based on checkstyle-noframe-sorted.xsl by Stephane Bailliez                   -->
    <!--  <sbailliez@apache.org> and sf-patch 1721291 by Leo Liang)                     -->
    <!-- Changes: 																								                      -->
    <!--  * Outputs seperate columns for error/warning/info                             -->
    <!--  * Sorts primarily by #error, secondarily by #warning, tertiary by #info       -->
    <!--  * Compatible with windows path names (converts '\' to '/' for html anchor)    -->
    <!--                                                                                -->
    <!-- Part of the Checkstyle distribution found at http://checkstyle.sourceforge.net -->
    <!-- Usage (generates checkstyle_report.html):                                      -->
    <!--    <checkstyle failonviolation="false" config="${check.config}">               -->
    <!--      <fileset dir="${src.dir}" includes="**/*.java"/>                          -->
    <!--      <formatter type="xml" toFile="${doc.dir}/checkstyle_report.xml"/>         -->
    <!--    </checkstyle>                                                               -->
    <!--    <style basedir="${doc.dir}" destdir="${doc.dir}"                            -->
    <!--            includes="checkstyle_report.xml"                                    -->
    <!--            style="${doc.dir}/checkstyle-noframes-severity-sorted.xsl"/>        -->

    <xsl:template match="checkstyle">
        <html>
            <head>
                <style type="text/css">
                    body {
                    font-family: Roboto, "Helvetica Neue", "Open Sans", "Segoe UI", Arial, sans-serif;
                    font-size: 1rem;
                    font-weight: 400;
                    line-height: 1.5;
                    color: #292b2c;
                    background-color: #fff;
                    margin: 1rem;
                    }

                    a {
                    color: #0275d8;
                    text-decoration: none;
                    }

                    a:focus, a:hover {
                    color: #014c8c;
                    text-decoration: underline;
                    }

                    a:active, a:hover {
                    outline-width: 0;
                    }

                    h1, h2, h3, h4, h5, h6 {
                    margin-bottom: .5rem;
                    font-family: inherit;
                    font-weight: 500;
                    line-height: 1.1;
                    color: inherit;
                    }

                    h1 {
                    font-size: 1.75rem;
                    }

                    h2 {
                    font-size: 1.5rem;
                    }

                    h3 {
                    font-size: 1.25rem;
                    }

                    table {
                    border-collapse: collapse;
                    }

                    table, th, td {
                    border: none;
                    width: 100%;
                    max-width: 100%;
                    }

                    table td, table th {
                    padding: .3rem;
                    vertical-align: top;
                    border-top: 1px solid #eceeef;
                    }

                    th, td {
                    text-align: left;
                    vertical-align: top;
                    }

                    th {
                    font-weight: bold;
                    background: #ccc;
                    color: black;
                    }

                    .table-striped tbody tr:nth-of-type(odd) {
                    background-color: rgba(0, 0, 0, .05);
                    }

                    table.log tr td, tr th {

                    }
                </style>
            </head>
            <body>
                <a name="top"/>
                <h1>Checkstyle report</h1>

                <!-- Summary part -->
                <xsl:apply-templates select="." mode="summary"/>

                <!-- Package List part -->
                <xsl:apply-templates select="." mode="filelist"/>

                <!-- For each package create its part -->
                <xsl:apply-templates select="file[@name and generate-id(.) = generate-id(key('files', @name))]"/>
            </body>
        </html>
    </xsl:template>


    <xsl:template match="checkstyle" mode="filelist">
        <h3>Files</h3>
        <table class="log table-striped">
            <tr>
                <th>Name</th>
                <th>Errors</th>
            </tr>
            <xsl:for-each select="file[@name and generate-id(.) = generate-id(key('files', @name))]">

                <xsl:sort data-type="number" order="descending" select="count(key('files', @name)/error[@severity='error'])"/>

                <xsl:variable name="errorCount" select="count(key('files', @name)/error[@severity='error'])"/>

                <xsl:choose>
                    <xsl:when test="$errorCount &gt; 0">
                        <tr>
                            <td>
                                <a href="#f-{translate(@name,'\','/')}">
                                    <xsl:value-of select="@name"/>
                                </a>
                            </td>
                            <td>
                                <xsl:value-of select="$errorCount"/>
                            </td>
                        </tr>
                    </xsl:when>
                </xsl:choose>
            </xsl:for-each>
        </table>
    </xsl:template>


    <xsl:template match="file">

        <xsl:variable name="errorCount" select="count(key('files', @name)/error[@severity='error'])"/>

        <xsl:choose>
            <xsl:when test="$errorCount &gt; 0">
                <a name="f-{translate(@name,'\','/')}"></a>
                <h3>
                    <xsl:value-of select="@name"/>
                </h3>

                <table class="log table-striped">
                    <tr>
                        <th>Error Description</th>
                        <th>Line</th>
                    </tr>
                    <xsl:for-each select="key('files', @name)/error">
                        <xsl:sort data-type="number" order="ascending" select="@line"/>
                        <tr>
                            <td>
                                <xsl:value-of select="@message"/>
                            </td>
                            <td>
                                <xsl:value-of select="@line"/>
                            </td>
                        </tr>
                    </xsl:for-each>
                </table>
                <a href="#top">Back to top</a>
            </xsl:when>
        </xsl:choose>
    </xsl:template>


    <xsl:template match="checkstyle" mode="summary">
        <xsl:variable name="errorCount" select="count(file/error[@severity='error'])"/>

        <strong>Errors: </strong>
        <xsl:value-of select="$errorCount"/>
    </xsl:template>
</xsl:stylesheet>

