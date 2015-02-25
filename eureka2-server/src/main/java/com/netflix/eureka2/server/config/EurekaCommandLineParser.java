/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.eureka2.server.config;

import java.util.List;

import com.netflix.eureka2.registry.datacenter.LocalDataCenterInfo.DataCenterType;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * @author Tomasz Bak
 */
public abstract class EurekaCommandLineParser<C extends EurekaCommonConfig, B extends EurekaCommonConfig.EurekaCommonConfigBuilder<C, B>> {

    protected final B builder;
    private final boolean resolverRequired;
    private final String[] args;

    private final Options options;

    protected boolean helpOption;

    protected EurekaCommandLineParser(B builder, boolean resolverRequired, String... args) {
        this.builder = builder;
        this.resolverRequired = resolverRequired;
        this.args = args;
        this.options = new Options()
                .addOption("h", false, "print this help information")
                .addOption("n", true, "server instance name")
                .addOption("d", true, "datacenter type (AWS|Basic). Default Basic")
                .addOption("q", true, "server resolver type (dns|fixed); default inline")
                .addOption("s", true, "shutdown port; default 7700")
                .addOption("a", true, "admin port; default 8077");
    }

    protected abstract void additionalOptions(Options options);

    @SuppressWarnings("unchecked")
    protected void process(CommandLine cli) {
        helpOption = cli.hasOption('h');
        if (!helpOption) {
            builder.withVipAddress(cli.getOptionValue("n"));
            builder.withDataCenterType(DataCenterType.valueOf(cli.getOptionValue("d", "Basic")));

            if (resolverRequired || !cli.getArgList().isEmpty()) {
                String resolverType = cli.getOptionValue("q", "fixed");
                builder.withResolverType(resolverType);
                switch (resolverType) {
                    case "dns":
                        if (cli.getArgList().size() != 1) {
                            throw new IllegalArgumentException("provide Eureka Write cluster <domain_name:reg:disc:repl> as parameter");
                        }
                        break;
                    case "fixed":
                        if (cli.getArgList().size() < 1) {
                            throw new IllegalArgumentException("provide Eureka Write cluster server <addresse:reg:disc:repl>s as parameter list");
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("resolver type not defined ('-q dns|fixed')");
                }
            }

            builder.withShutDownPort(Integer.parseInt(cli.getOptionValue("s", "7700")));
            if (!cli.hasOption("n")) {
                throw new IllegalArgumentException("missing required server name option ('-n <server_name>')");
            }
            builder.withAppName(cli.getOptionValue("n"));
            builder.withWebAdminPort(Integer.parseInt(cli.getOptionValue("a", "8077")));
            builder.withServerList((((List<String>) cli.getArgList()).toArray(new String[cli.getArgList().size()])));
        }
    }

    public boolean hasHelpOption() {
        return helpOption;
    }

    public void printHelp() {
        new HelpFormatter().printHelp("syntax", options);
    }

    public C process() {
        additionalOptions(options);
        try {
            CommandLine cli = new PosixParser().parse(options, args);
            process(cli);
            return builder.build();
        } catch (ParseException e) {
            throw new IllegalArgumentException("invalid command line parameters; " + e);
        }
    }
}