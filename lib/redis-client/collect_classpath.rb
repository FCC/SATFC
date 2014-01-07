ROOT = "../../"

puts(File.readlines("#{ROOT}.classpath").map do |line|
  next unless line =~ %r{<classpathentry.*?path="(.*)"/>\n}
  path = $1
  next if path =~ %r{^org.eclipse.jdt.launching.} # JRE.
  next if path =~ %r{^lib/aclib-experimental/spi-.*.jar} # a nasty preprocessor that breaks everything.
  ROOT+path
end.compact.join(":"))