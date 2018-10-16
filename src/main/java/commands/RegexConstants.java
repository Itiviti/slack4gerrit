/*************************************************************************
 * ULLINK CONFIDENTIAL INFORMATION
 * _______________________________
 *
 * All Rights Reserved.
 *
 * NOTICE: This file and its content are the property of Ullink. The
 * information included has been classified as Confidential and may
 * not be copied, modified, distributed, or otherwise disseminated, in
 * whole or part, without the express written permission of Ullink.
 *************************************************************************/
package commands;

public class RegexConstants
{
    public static final String CHANNEL = "[\\w-_]+";

    private RegexConstants(){}

    public static final String SPACES = "\\s+";
    public static final String CHANGE_ID = "\\d+";
    public static final String PROJECT = "[^@]\\W*";

    public static final String USER_ALIAS = "@\\S*";
    public static final String ANYTHING_ELSE = ".*";
    public static final String COMMENT = ANYTHING_ELSE;

}
